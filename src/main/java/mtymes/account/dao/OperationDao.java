package mtymes.account.dao;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
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

    private final MongoCollection<Document> operations;
    private final OperationDbMapper mapper = new OperationDbMapper();

    private final StampedLock seqGenerationLock = new StampedLock();

    public OperationDao(MongoCollection<Document> operations) {
        this.operations = operations;
    }

    public OperationId storeOperation(Operation operation) {
        long sequenceId = storeWithSequenceId(
                docBuilder()
                        .put("type", operation.type())
                        .put("accountIds", operation.affectedAccountIds())
                        .put("body", operation.apply(mapper))
                        .build()
        );
        return operationId(sequenceId);
    }

    // todo: test
    public void markAsSuccessful(OperationId operationId) {
        operations.updateOne(
                docBuilder()
                        .put("_id", operationId)
                        .put("finalState", null)
                        .build(),
                doc("$set", doc("finalState", "success"))
        );
    }

    // todo: test
    public void markAsFailed(OperationId operationId, String description) {
        operations.updateOne(
                docBuilder()
                        .put("_id", operationId)
                        .put("finalState", null)
                        .build(),
                doc("$set", docBuilder()
                        .put("finalState", "failure")
                        .put("description", description)
                        .build())
        );
    }

    public Optional<PersistedOperation> findOperation(OperationId operationId) {
        return findOne(
                operations,
                doc("_id", operationId),
                doc -> {
                    Operation operation = mapper.toOperation(
                            doc.getString("type"),
                            (Document) doc.get("body")
                    );
                    Optional<FinalState> finalState = Optional.ofNullable(doc.getString("finalState")).map(state -> {
                        if ("success".equals(state)) {
                            return FinalState.Success;
                        } else if ("failure".equals(state)) {
                            return FinalState.Failure;
                        } else {
                            throw new IllegalStateException(String.format("Unknown state '%s'", state));
                        }
                    });
                    return new PersistedOperation(
                            operationId(doc.getLong("_id")),
                            operation,
                            finalState,
                            Optional.ofNullable(doc.getString("description"))
                    );
                }
        );
    }

    // todo: test this
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
                    document.put("_id", idToUse);
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
                .find().projection(doc("_id", 1)).sort(doc("_id", -1)).limit(1)
                .iterator();
        return idIterator.hasNext() ? idIterator.next().getLong("_id") : 0;
    }
}
