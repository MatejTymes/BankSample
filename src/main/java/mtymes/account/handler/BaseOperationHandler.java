package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Operation;
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

    protected void markAsSuccess(SeqId seqId) {
        operationDao.markAsSuccessful(seqId);
    }

    protected void markAsFailure(SeqId seqId, String description) {
        operationDao.markAsFailed(seqId, description);
    }
}
