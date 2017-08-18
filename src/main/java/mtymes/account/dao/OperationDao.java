package mtymes.account.dao;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.UpdateResult;
import mtymes.account.domain.operation.FinalState;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.PersistedOperation;
import org.bson.Document;

import java.util.Optional;
import java.util.concurrent.locks.StampedLock;

import static mtymes.account.domain.operation.OperationId.operationId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class OperationDao extends BaseDao {


    public static final String _ID = "_id";
    public static final String TYPE = "type";
    public static final String ACCOUNT_IDS = "accountIds";
    public static final String BODY = "body";
    public static final String FINAL_STATE = "finalState";
    public static final String DESCRIPTION = "description";

    private final MongoCollection<Document> operations;
    private final OperationDbMapper mapper = new OperationDbMapper();

    private final StampedLock seqGenerationLock = new StampedLock();

    public OperationDao(MongoCollection<Document> operations) {
        this.operations = operations;
    }

    public OperationId storeOperation(Operation operation) {
        long sequenceId = storeWithSequenceId(
                docBuilder()
                        .put(TYPE, operation.type())
                        .put(ACCOUNT_IDS, operation.affectedAccountIds())
                        .put(BODY, operation.apply(mapper))
                        .build()
        );
        return operationId(sequenceId);
    }

    public boolean markAsSuccessful(OperationId operationId) {
        UpdateResult result = operations.updateOne(
                docBuilder()
                        .put(_ID, operationId)
                        .put(FINAL_STATE, null)
                        .build(),
                doc("$set", doc(FINAL_STATE, "success"))
        );
        return result.getModifiedCount() == 1;
    }

    public boolean markAsFailed(OperationId operationId, String description) {
        UpdateResult result = operations.updateOne(
                docBuilder()
                        .put(_ID, operationId)
                        .put(FINAL_STATE, null)
                        .build(),
                doc("$set", docBuilder()
                        .put(FINAL_STATE, "failure")
                        .put(DESCRIPTION, description)
                        .build())
        );
        return result.getModifiedCount() == 1;
    }

    public Optional<PersistedOperation> findOperation(OperationId operationId) {
        return findOne(
                operations,
                doc(_ID, operationId),
                doc -> {
                    Operation operation = mapper.toOperation(
                            doc.getString(TYPE),
                            (Document) doc.get(BODY)
                    );
                    Optional<FinalState> finalState = Optional.ofNullable(doc.getString(FINAL_STATE)).map(state -> {
                        if ("success".equals(state)) {
                            return FinalState.Success;
                        } else if ("failure".equals(state)) {
                            return FinalState.Failure;
                        } else {
                            throw new IllegalStateException(String.format("Unknown state '%s'", state));
                        }
                    });
                    return new PersistedOperation(
                            operationId(doc.getLong(_ID)),
                            operation,
                            finalState,
                            Optional.ofNullable(doc.getString(DESCRIPTION))
                    );
                }
        );
    }

    // using "Optimistic Loop" to guarantee the sequencing of Operations
    // look at: https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/ for more details
    private long storeWithSequenceId(Document document) {
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
                    retry = true;
                    if (++attemptCount < 3) {
                        idToUse++;
                    } else {
                        attemptCount = 0;
                        idToUse = getLastId() + 1;
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
}
