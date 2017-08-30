package mtymes.account.handler;

import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.Operation;

public interface OperationHandler<T extends Operation> {

    void handleOperation(OpLogId opLogId, T request);
}
