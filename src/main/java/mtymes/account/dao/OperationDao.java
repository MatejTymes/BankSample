package mtymes.account.dao;

import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.PersistedOperation;

import java.util.Optional;

public interface OperationDao {

    OperationId storeOperation(Operation operation);

    boolean markAsSuccessful(OperationId operationId);

    boolean markAsFailed(OperationId operationId, String description);

    Optional<PersistedOperation> findOperation(OperationId operationId);
}
