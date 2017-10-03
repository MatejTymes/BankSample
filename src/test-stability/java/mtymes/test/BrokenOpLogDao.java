package mtymes.test;

import javafixes.object.Tuple;
import mtymes.account.dao.OpLogDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;

import java.util.List;
import java.util.function.Supplier;

public class BrokenOpLogDao extends BrokenClass implements OpLogDao {

    private final OpLogDao wrappedDao;

    public BrokenOpLogDao(OpLogDao wrappedDao, Supplier<RuntimeException> exceptionSupplier) {
        super(exceptionSupplier);
        this.wrappedDao = wrappedDao;
    }

    @Override
    public SeqId registerOperationId(AccountId accountId, OperationId operationId) {
        failTheFirstTime("registerOperationId", accountId, operationId);
        return wrappedDao.registerOperationId(accountId, operationId);
    }

    @Override
    public void markAsFinished(OperationId operationId) {
        failTheFirstTime("markAsFinished", operationId);
        wrappedDao.markAsFinished(operationId);
    }

    @Override
    public List<Tuple<OperationId, SeqId>> findUnfinishedOperationIds(AccountId accountId) {
        failTheFirstTime("findUnfinishedOperationIds", accountId);
        return wrappedDao.findUnfinishedOperationIds(accountId);
    }
}
