package mtymes.account.dao.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import javafixes.object.Tuple;
import mtymes.account.dao.OpLogDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.exception.DuplicateItemException;
import org.bson.Document;

import java.util.List;

import static javafixes.common.CollectionUtil.newList;
import static javafixes.object.Tuple.tuple;
import static mtymes.account.domain.operation.SeqId.seqId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class MongoOpLogDao extends MongoBaseDao implements OpLogDao {

    private static final int DUPLICATE_CODE = 11000;

    public static final String ACCOUNT_ID = "accId";
    public static final String SEQ_ID = "seqId";
    public static final String OPERATION_ID = "opId";
    public static final String FINISHED = "finished";

    private final MongoCollection<Document> opLogs;
    private final MongoMapper mapper = new MongoMapper();

    public MongoOpLogDao(MongoCollection<Document> opLogs) {
        this.opLogs = opLogs;
    }

    @Override
    public SeqId registerOperationId(AccountId accountId, OperationId operationId) {
        long seqIdValue = storeWithSequenceId(
                accountId,
                docBuilder()
                        .put(ACCOUNT_ID, accountId)
                        .put(OPERATION_ID, operationId)
                        .build()
        );
        return seqId(seqIdValue);
    }

    @Override
    public void markAsFinished(OperationId operationId) {
        opLogs.updateOne(doc(OPERATION_ID, operationId), doc("$set", doc(FINISHED, true)));
    }

    @Override
    public List<Tuple<OperationId, SeqId>> findUnfinishedOperationIds(AccountId accountId) {
        MongoIterable<Tuple<OperationId, SeqId>> operationIds = opLogs.find(
                docBuilder()
                        .put(ACCOUNT_ID, accountId)
                        .put(FINISHED, null)
                        .build()
        ).projection(
                docBuilder()
                        .put(ACCOUNT_ID, 1)
                        .put(SEQ_ID, 1)
                        .put(OPERATION_ID, 1)
                        .build()

        ).sort(
                doc(SEQ_ID, 1)
        ).map(doc -> tuple(
                mapper.getOperationId(doc, OPERATION_ID),
                mapper.getSeqId(doc, SEQ_ID)
        ));
        return newList(operationIds);
    }

    // using "Optimistic Loop" to guarantee the sequencing of Operations
    // look at: https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/ for more details
    private long storeWithSequenceId(AccountId accountId, Document document) throws DuplicateItemException {
        long idToUse;

        int attemptCount = 0; // use of this is relevant only in case of multi-node scenario

        idToUse = getLastSeqId(accountId) + 1;
        boolean retry;
        do {
            retry = false;
            try {
                document.put(SEQ_ID, idToUse);
                opLogs.insertOne(document);
            } catch (MongoWriteException e) {
                if (e.getError().getCode() == DUPLICATE_CODE) {
                    if (e.getError().getMessage().contains(SEQ_ID)) {
                        retry = true;
                        if (++attemptCount < 3) {
                            idToUse++;
                        } else {
                            attemptCount = 0;
                            idToUse = getLastSeqId(accountId) + 1;
                        }
                    } else {
                        throw new DuplicateItemException(e);
                    }
                } else {
                    throw e;
                }
            }
        } while (retry);

        return idToUse;
    }

    private long getLastSeqId(AccountId accountId) {
        MongoCursor<Document> idIterator = opLogs
                .find(doc(ACCOUNT_ID, accountId)).projection(doc(SEQ_ID, 1)).sort(doc(SEQ_ID, -1)).limit(1)
                .iterator();
        return idIterator.hasNext() ? idIterator.next().getLong(SEQ_ID) : 0;
    }
}
