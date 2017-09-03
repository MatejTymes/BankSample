package mtymes.test;

import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.exception.DuplicateOperationException;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class BrokenOperationDao extends BrokenClass implements OperationDao {

    private final OperationDao wrappedDao;

    public BrokenOperationDao(OperationDao wrappedDao, Supplier<RuntimeException> exceptionSupplier) {
        super(exceptionSupplier);
        this.wrappedDao = wrappedDao;
    }

    @Override
    public OpLogId storeOperation(Operation operation) throws DuplicateOperationException {
        failTheFirstTime("storeOperation", operation);
        return wrappedDao.storeOperation(operation);
    }

    @Override
    public boolean markAsSuccessful(OpLogId opLogId) {
        failTheFirstTime("markAsSuccessful", opLogId);
        return wrappedDao.markAsSuccessful(opLogId);
    }

    @Override
    public boolean markAsFailed(OpLogId opLogId, String description) {
        failTheFirstTime("markAsFailed", opLogId, description);
        return wrappedDao.markAsFailed(opLogId, description);
    }

    @Override
    public Optional<LoggedOperation> findLoggedOperation(OpLogId opLogId) {
        failTheFirstTime("findLoggedOperation", opLogId);
        return wrappedDao.findLoggedOperation(opLogId);
    }

    @Override
    public List<OpLogId> findUnfinishedOperationLogIds(AccountId accountId) {
        failTheFirstTime("findUnfinishedOperationLogIds", accountId);
        return wrappedDao.findUnfinishedOperationLogIds(accountId);
    }
}
