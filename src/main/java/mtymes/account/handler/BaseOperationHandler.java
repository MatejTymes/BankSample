package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;

import static java.lang.String.format;
import static mtymes.account.handler.BaseOperationHandler.Progress.*;

public abstract class BaseOperationHandler<T extends Operation> implements OperationHandler<T> {

    protected final AccountDao accountDao;
    protected final OperationDao operationDao;

    protected BaseOperationHandler(AccountDao accountDao, OperationDao operationDao) {
        this.accountDao = accountDao;
        this.operationDao = operationDao;
    }

    protected Progress checkProgress(AccountId accountId, OperationId operationId) throws IllegalStateException {
        OperationId lastAppliedOperationId = accountDao
                .findLastAppliedOperationId(accountId)
                .orElseThrow(
                        () -> new IllegalStateException(format("Failed to load OperationId for Account '%s'", accountId))
                );

        int comparison = lastAppliedOperationId.compareTo(operationId);
        return comparison < 0 ? OlderOperationApplied : comparison == 0 ? ThisOperationApplied : NewerOperationApplied;
    }

    protected Account loadAccount(AccountId accountId) {
        return accountDao
                .findAccount(accountId)
                .orElseThrow(
                        () -> new IllegalStateException(format("Failed to load Account '%s'", accountId))
                );
    }

    protected void markAsSuccess(OperationId operationId) {
        operationDao.markAsSuccessful(operationId);
    }

    protected void markAsFailure(OperationId operationId, String description) {
        operationDao.markAsFailed(operationId, description);
    }

    protected enum Progress {
        OlderOperationApplied,
        ThisOperationApplied,
        NewerOperationApplied
    }
}