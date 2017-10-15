package mtymes.account.dao.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.operation.FinalState;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.exception.DuplicateItemException;
import org.bson.Document;

import java.util.Optional;

import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class MongoOperationDao extends MongoBaseDao implements OperationDao {

    private static final int DUPLICATE_CODE = 11000;

    public static final String OPERATION_ID = "operationId";
    public static final String TYPE = "type";
    public static final String BODY = "body";
    public static final String FINAL_STATE = "finalState";
    private static final String DESCRIPTION = "description";

    private final MongoCollection<Document> operations;
    private final MongoMapper mapper = new MongoMapper();

    public MongoOperationDao(MongoCollection<Document> operations) {
        this.operations = operations;
    }

    @Override
    public void storeOperation(Operation operation) {
        try {
            operations.insertOne(docBuilder()
                    .put(OPERATION_ID, operation.operationId)
                    .put(TYPE, operation.type())
                    .put(BODY, operation.apply(mapper))
                    .build());
        } catch (MongoWriteException e) {
            if (e.getError().getCode() == DUPLICATE_CODE) {
                throw new DuplicateItemException(e);
            } else {
                throw e;
            }
        }
    }

    @Override
    public boolean markAsApplied(OperationId operationId) {
        return markAsFinished(operationId, Applied, Optional.empty());
    }

    @Override
    public boolean markAsRejected(OperationId operationId, String description) {
        return markAsFinished(operationId, Rejected, Optional.of(description));
    }

    @Override
    public Optional<LoggedOperation> findLoggedOperation(OperationId operationId) {
        return findOne(
                operations,
                doc(OPERATION_ID, operationId),
                this::toPersistedOperation
        );
    }

    private boolean markAsFinished(OperationId operationId, FinalState state, Optional<String> description) {
        UpdateResult result = operations.updateOne(
                docBuilder()
                        .put(OPERATION_ID, operationId)
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
                mapper.toOperation(
                        mapper.getOperationId(doc, OPERATION_ID),
                        doc.getString(TYPE),
                        (Document) doc.get(BODY)
                ),
                mapper.getOptionalFinalState(doc, FINAL_STATE),
                mapper.getOptionalString(doc, DESCRIPTION)
        );
    }
}
