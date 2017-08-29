package mtymes.account.dao.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.UpdateResult;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.operation.FinalState;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.PersistedOperation;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.exception.DuplicateOperationException;
import org.bson.Document;

import java.util.Optional;
import java.util.concurrent.locks.StampedLock;

import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;
import static mtymes.account.domain.operation.SeqId.seqId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class MongoOperationDao extends MongoBaseDao implements OperationDao {

    private static final String _ID = "_id";
    private static final String TYPE = "type";
    private static final String ACCOUNT_IDS = "accountIds";
    private static final String BODY = "body";
    private static final String FINAL_STATE = "finalState";
    private static final String DESCRIPTION = "description";
    public static final int DUPLICATE = 11000;

    private final MongoCollection<Document> operations;
    private final OperationDbMapper mapper = new OperationDbMapper();

    private final StampedLock seqGenerationLock = new StampedLock();

    public MongoOperationDao(MongoCollection<Document> operations) {
        this.operations = operations;
    }

    @Override
    public SeqId storeOperation(Operation operation) throws DuplicateOperationException {
        long sequenceId = storeWithSequenceId(
                // todo: move this into OperationDbMapper
                docBuilder()
                        .put(TYPE, operation.type())
                        .put(ACCOUNT_IDS, operation.affectedAccountIds())
                        .put(BODY, operation.apply(mapper))
                        .build()
        );
        return seqId(sequenceId);
    }

    @Override
    public boolean markAsSuccessful(SeqId seqId) {
        return markAsFinished(seqId, Success, Optional.empty());
    }

    @Override
    public boolean markAsFailed(SeqId seqId, String description) {
        return markAsFinished(seqId, Failure, Optional.of(description));
    }

    @Override
    public Optional<PersistedOperation> findOperation(SeqId seqId) {
        return findOne(
                operations,
                doc(_ID, seqId),
                this::toPersistedOperation
        );
    }

    // using "Optimistic Loop" to guarantee the sequencing of Operations
    // look at: https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/ for more details
    private long storeWithSequenceId(Document document) throws DuplicateOperationException {
        long idToUse;

        int attemptCount = 0; // use of this is relevant only in case of multi-node scenario

        long stamp = seqGenerationLock.writeLock();
        try {
            idToUse = getLastId() + 1;
            boolean retry;
            do {
                retry = false;
                try {
                    document.put(_ID, idToUse);
                    operations.insertOne(document);
                } catch (MongoWriteException e) {
                    if (e.getError().getCode() == DUPLICATE) {
                        if (e.getError().getMessage().contains("_id")) {
                            retry = true;
                            if (++attemptCount < 3) {
                                idToUse++;
                            } else {
                                attemptCount = 0;
                                idToUse = getLastId() + 1;
                            }
                        } else {
                            throw new DuplicateOperationException(e);
                        }
                    } else {
                        throw e;
                    }
                }
            } while (retry);
        } finally {
            seqGenerationLock.unlock(stamp);
        }

        return idToUse;
    }

    private long getLastId() {
        MongoCursor<Document> idIterator = operations
                .find().projection(doc(_ID, 1)).sort(doc(_ID, -1)).limit(1)
                .iterator();
        return idIterator.hasNext() ? idIterator.next().getLong(_ID) : 0;
    }

    private boolean markAsFinished(SeqId seqId, FinalState state, Optional<String> description) {
        UpdateResult result = operations.updateOne(
                docBuilder()
                        .put(_ID, seqId)
                        .put(FINAL_STATE, null)
                        .build(),
                doc("$set", docBuilder()
                        .put(FINAL_STATE, state)
                        .put(DESCRIPTION, description)
                        .build())
        );
        return result.getModifiedCount() == 1;
    }

    private PersistedOperation toPersistedOperation(Document doc) {
        Operation operation = mapper.toOperation(
                doc.getString(TYPE),
                (Document) doc.get(BODY)
        );
        Optional<FinalState> finalState = Optional.ofNullable(doc.getString(FINAL_STATE)).map(FinalState::valueOf);
        return new PersistedOperation(
                seqId(doc.getLong(_ID)),
                operation,
                finalState,
                Optional.ofNullable(doc.getString(DESCRIPTION))
        );
    }
}
