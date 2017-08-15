package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.operation.InternalTransfer;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;

public class InternalTransferHandler extends OperationHandler<InternalTransfer> {

    public InternalTransferHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test this
    @Override
    boolean canHandleRequest(Operation operation) {
        return operation instanceof InternalTransfer;
    }

    // todo: test
    // todo: test that any dao interaction can fail
    @Override
    void handleRequest(OperationId operationId, InternalTransfer request) {

    }
}
