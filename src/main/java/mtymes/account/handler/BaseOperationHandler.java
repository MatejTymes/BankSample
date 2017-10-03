package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;

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

    protected Optional<SeqId> loadAccountVersion(AccountId accountId) {
        return accountDao.findCurrentVersion(accountId);
    }

    protected void markOperationAsApplied(OperationId operationId) {
        operationDao.markAsApplied(operationId);
    }

    protected void markOperationAsRejected(OperationId operationId, String description) {
        operationDao.markAsRejected(operationId, description);
    }
}
