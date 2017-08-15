package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.RequestDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;

import static java.lang.String.format;
import static mtymes.account.handler.OperationHandler.Progress.NewerOperationApplied;
import static mtymes.account.handler.OperationHandler.Progress.OlderOperationApplied;
import static mtymes.account.handler.OperationHandler.Progress.ThisOperationApplied;

public abstract class OperationHandler<T extends Operation> {

    protected final AccountDao accountDao;
    protected final RequestDao requestDao;

    protected OperationHandler(AccountDao accountDao, RequestDao requestDao) {
        this.accountDao = accountDao;
        this.requestDao = requestDao;
    }

    abstract boolean canHandleRequest(Operation operation);

    abstract void handleRequest(OperationId operationId, T request);

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
        requestDao.markAsSuccessful(operationId);
    }

    protected void markAsFailure(OperationId operationId, String description) {
        requestDao.markAsFailed(operationId, description);
    }

    protected enum Progress {
        OlderOperationApplied,
        ThisOperationApplied,
        NewerOperationApplied
    }
}
