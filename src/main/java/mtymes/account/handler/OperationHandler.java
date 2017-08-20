package mtymes.account.handler;

import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;

public interface OperationHandler<T extends Operation> {

    void handleOperation(OperationId operationId, T request);
}
