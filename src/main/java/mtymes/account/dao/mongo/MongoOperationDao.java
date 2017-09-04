package mtymes.account.dao.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.UpdateResult;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.FinalState;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.exception.DuplicateOperationException;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

import static javafixes.common.CollectionUtil.newList;
import static mtymes.account.domain.account.Version.version;
import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.account.domain.operation.OpLogId.opLogId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class MongoOperationDao extends MongoBaseDao implements OperationDao {

    private static final int DUPLICATE_CODE = 11000;

    public static final String VERSION = "version";
    public static final String TYPE = "type";
    public static final String ACCOUNT_ID = "accountId";
    private static final String BODY = "body";
    public static final String FINAL_STATE = "finalState";
    private static final String DESCRIPTION = "description";

    private final MongoCollection<Document> operations;
    private final MongoMapper mapper = new MongoMapper();

    public MongoOperationDao(MongoCollection<Document> operations) {
        this.operations = operations;
    }

    @Override
    public OpLogId storeOperation(Operation operation) throws DuplicateOperationException {
        AccountId accountId = operation.affectedAccountId();
        long sequenceId = storeWithSequenceId(
                accountId,
                docBuilder()
                        .put(TYPE, operation.type())
                        .put(ACCOUNT_ID, accountId)
                        .put(BODY, operation.apply(mapper))
                        .build()
        );
        return opLogId(accountId, version(sequenceId));
    }

    @Override
    public boolean markAsApplied(OpLogId opLogId) {
        return markAsFinished(opLogId, Applied, Optional.empty());
    }

    @Override
    public boolean markAsRejected(OpLogId opLogId, String description) {
        return markAsFinished(opLogId, Rejected, Optional.of(description));
    }

    @Override
    public Optional<LoggedOperation> findLoggedOperation(OpLogId opLogId) {
        return findOne(
                operations,
                docBuilder()
                        .put(ACCOUNT_ID, opLogId.accountId)
                        .put(VERSION, opLogId.seqId)
                        .build(),
                this::toPersistedOperation
        );
    }

    @Override
    public List<OpLogId> findUnfinishedOperationLogIds(AccountId accountId) {
        MongoIterable<OpLogId> opLogIds = operations.find(
                docBuilder()
                        .put(ACCOUNT_ID, accountId)
                        .put(FINAL_STATE, null)
                        .build()
        ).projection(
                docBuilder()
                        .put(ACCOUNT_ID, 1)
                        .put(VERSION, 1)
                        .build()
        ).sort(
                doc(VERSION, 1)
        ).map(doc -> opLogId(
                mapper.getAccountId(doc, ACCOUNT_ID),
                mapper.getVersion(doc, VERSION)
        ));
        return newList(opLogIds);
    }

    // using "Optimistic Loop" to guarantee the sequencing of Operations
    // look at: https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/ for more details
    private long storeWithSequenceId(AccountId accountId, Document document) throws DuplicateOperationException {
        long idToUse;

        int attemptCount = 0; // use of this is relevant only in case of multi-node scenario

        idToUse = getLastVersion(accountId) + 1;
        boolean retry;
        do {
            retry = false;
            try {
                document.put(VERSION, idToUse);
                operations.insertOne(document);
            } catch (MongoWriteException e) {
                if (e.getError().getCode() == DUPLICATE_CODE) {
                    if (e.getError().getMessage().contains(VERSION)) {
                        retry = true;
                        if (++attemptCount < 3) {
                            idToUse++;
                        } else {
                            attemptCount = 0;
                            idToUse = getLastVersion(accountId) + 1;
                        }
                    } else {
                        throw new DuplicateOperationException(e);
                    }
                } else {
                    throw e;
                }
            }
        } while (retry);

        return idToUse;
    }

    private long getLastVersion(AccountId accountId) {
        MongoCursor<Document> idIterator = operations
                .find(doc(ACCOUNT_ID, accountId)).projection(doc(VERSION, 1)).sort(doc(VERSION, -1)).limit(1)
                .iterator();
        return idIterator.hasNext() ? idIterator.next().getLong(VERSION) : 0;
    }

    private boolean markAsFinished(OpLogId opLogId, FinalState state, Optional<String> description) {
        UpdateResult result = operations.updateOne(
                docBuilder()
                        .put(ACCOUNT_ID, opLogId.accountId)
                        .put(VERSION, opLogId.seqId)
                        .put(FINAL_STATE, null)
                        .build(),
                doc("$set", docBuilder()
                        .put(FINAL_STATE, state)
                        .put(DESCRIPTION, description)
                        .build())
        );
        return result.getModifiedCount() == 1;
    }

    private LoggedOperation toPersistedOperation(Document doc) {
        return new LoggedOperation(
                opLogId(
                        mapper.getAccountId(doc, ACCOUNT_ID),
                        mapper.getVersion(doc, VERSION)
                ),
                mapper.toOperation(
                        doc.getString(TYPE),
                        (Document) doc.get(BODY)
                ),
                mapper.getOptionalFinalState(doc, FINAL_STATE),
                mapper.getOptionalString(doc, DESCRIPTION)
        );
    }
}
