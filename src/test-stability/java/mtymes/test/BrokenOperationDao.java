package mtymes.test;

import mtymes.account.dao.OperationDao;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.exception.DuplicateItemException;

import java.util.Optional;
import java.util.function.Supplier;

public class BrokenOperationDao extends BrokenClass implements OperationDao {

    private final OperationDao wrappedDao;

    public BrokenOperationDao(OperationDao wrappedDao, Supplier<RuntimeException> exceptionSupplier) {
        super(exceptionSupplier);
        this.wrappedDao = wrappedDao;
    }

    @Override
    public void storeOperation(Operation operation) throws DuplicateItemException {
        failTheFirstTime("storeOperation", operation);
        wrappedDao.storeOperation(operation);
    }

    @Override
    public boolean markAsApplied(OperationId operationId) {
        failTheFirstTime("markAsApplied", operationId);
        return wrappedDao.markAsApplied(operationId);
    }

    @Override
    public boolean markAsRejected(OperationId operationId, String description) {
        failTheFirstTime("markAsRejected", operationId, description);
        return wrappedDao.markAsRejected(operationId, description);
    }

    @Override
    public Optional<LoggedOperation> findLoggedOperation(OperationId operationId) {
        failTheFirstTime("findLoggedOperation", operationId);
        return wrappedDao.findLoggedOperation(operationId);
    }
}
