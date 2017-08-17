package mtymes.account.dao;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;
import org.apache.commons.lang3.NotImplementedException;
import org.bson.Document;

import static mtymes.account.domain.operation.OperationId.operationId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class OperationDao {

    private final MongoCollection<Document> operations;
    private final MongoMapper mapper = new MongoMapper();

    public OperationDao(MongoCollection<Document> operations) {
        this.operations = operations;
    }

    // todo: test
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

    // todo: test
    private Document asDocument(Operation operation) {
        // todo: implement
        throw new NotImplementedException("implement me");
    }

    // todo: execute in single thread
    // using "Optimistic Loop" to guarantee the sequencing of Operations
    // look at: https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/ for more details
    private long storeWithSequenceId(Document document) {
        long idToUse = getLastId() + 1;

        boolean retry;
        do {
            retry = false;
            try {
                document.put("_id", idToUse);
                operations.insertOne(document);
            } catch (DuplicateKeyException e) {
                retry = true;
                idToUse++;
            }
        } while (retry);

        return idToUse;
    }

    private long getLastId() {
        MongoCursor<Document> idIterator = operations
                .find().projection(doc("_id", 1)).sort(doc("_id", -1)).limit(1)
                .iterator();
        return idIterator.hasNext() ? idIterator.next().getLong("_id") : 0;
    }
}
