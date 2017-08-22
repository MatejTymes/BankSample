package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;

public abstract class BaseOperationHandler<T extends Operation> implements OperationHandler<T> {

    protected final AccountDao accountDao;
    protected final OperationDao operationDao;

    protected BaseOperationHandler(AccountDao accountDao, OperationDao operationDao) {
        this.accountDao = accountDao;
        this.operationDao = operationDao;
    }

    protected void markAsSuccess(OperationId operationId) {
        operationDao.markAsSuccessful(operationId);
    }

    protected void markAsFailure(OperationId operationId, String description) {
        operationDao.markAsFailed(operationId, description);
    }
}
