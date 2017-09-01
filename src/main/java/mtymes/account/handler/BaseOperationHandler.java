package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.Operation;

import java.util.Optional;

public abstract class BaseOperationHandler<T extends Operation> implements OperationHandler<T> {

    protected final AccountDao accountDao;
    protected final OperationDao operationDao;

    protected BaseOperationHandler(AccountDao accountDao, OperationDao operationDao) {
        this.accountDao = accountDao;
        this.operationDao = operationDao;
    }

    protected Optional<Account> loadAccount(AccountId accountId) {
        return accountDao.findAccount(accountId);
    }

    protected Optional<Version> loadAccountVersion(AccountId accountId) {
        return accountDao.findCurrentVersion(accountId);
    }

    protected void markAsSuccess(OpLogId opLogId) {
        operationDao.markAsSuccessful(opLogId);
    }

    protected void markAsFailure(OpLogId opLogId, String description) {
        operationDao.markAsFailed(opLogId, description);
    }
}
