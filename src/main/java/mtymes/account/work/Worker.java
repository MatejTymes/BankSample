package mtymes.account.work;

import javafixes.object.Tuple;
import mtymes.account.dao.OpLogDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.handler.OperationDispatcher;

import java.util.List;

public class Worker {

    private final OpLogDao opLogDao;
    private final OperationDao operationDao;
    private final OperationDispatcher dispatcher;

    public Worker(OpLogDao opLogDao, OperationDao operationDao, OperationDispatcher dispatcher) {
        this.opLogDao = opLogDao;
        this.operationDao = operationDao;
        this.dispatcher = dispatcher;
    }

    public void runUnfinishedOperations(AccountId accountId) {
        List<Tuple<OperationId, SeqId>> unfinishedOpLogIds = opLogDao.findUnfinishedOperationIds(accountId);
        for (Tuple<OperationId, SeqId> unfinishedOpLogId : unfinishedOpLogIds) {
            LoggedOperation loggedOperation = operationDao.findLoggedOperation(unfinishedOpLogId.a).get();
            dispatcher.dispatchOperation(unfinishedOpLogId.b, loggedOperation);
        }
    }
}
