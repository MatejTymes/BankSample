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

    public OperationDao(MongoCollection<Document> operations) {
        this.operations = operations;
    }

    // todo: test
    public OperationId storeOperation(Operation operation) {
        long longId = storeOperation(
                operation.type(),
                asDocument(operation)
        );
        return operationId(longId);
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
    private long storeOperation(String type, Document operationDocument) {
        long idToUse = getLastId() + 1;

        boolean retry;
        do {
            retry = false;
            try {
                operations.insertOne(docBuilder()
                        .put("_id", idToUse)
                        .put("type", type)
                        .put("body", operationDocument)
                        .build());
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
