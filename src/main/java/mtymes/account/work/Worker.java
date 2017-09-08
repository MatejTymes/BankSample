package mtymes.account.work;

import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.handler.OperationDispatcher;

import java.util.List;

public class Worker {

    private final OperationDao operationDao;
    private final OperationDispatcher dispatcher;

    public Worker(OperationDao operationDao, OperationDispatcher dispatcher) {
        this.operationDao = operationDao;
        this.dispatcher = dispatcher;
    }

    // todo: test this
    public void runUnfinishedOperations(AccountId accountId) {
        List<OpLogId> unfinishedOpLogIds = operationDao.findUnfinishedOperationLogIds(accountId);
        for (OpLogId unfinishedOpLogId : unfinishedOpLogIds) {
            LoggedOperation loggedOperation = operationDao.findLoggedOperation(unfinishedOpLogId).get();
            dispatcher.dispatchOperation(loggedOperation);
        }
    }
}
