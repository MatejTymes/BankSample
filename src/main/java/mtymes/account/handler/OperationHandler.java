package mtymes.account.handler;

import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.SeqId;

public interface OperationHandler<T extends Operation> {

    void handleOperation(SeqId seqId, T request);
}
