package mtymes.account.dao;

import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;

import java.util.Optional;

public interface OperationDao {

    void storeOperation(Operation operation);

    boolean markAsApplied(OperationId operationId);

    boolean markAsRejected(OperationId operationId, String description);

    Optional<LoggedOperation> findLoggedOperation(OperationId operationId);
}
