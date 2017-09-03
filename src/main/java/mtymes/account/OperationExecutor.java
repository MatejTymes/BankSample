package mtymes.account;

import javafixes.math.Decimal;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.handler.HandlerDispatcher;

import java.util.List;

import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.domain.operation.TransferId.newTransferId;

// todo: test this
public class OperationExecutor {

    private final OperationDao operationDao;
    private final HandlerDispatcher dispatcher;

    public OperationExecutor(OperationDao operationDao, HandlerDispatcher dispatcher) {
        this.operationDao = operationDao;
        this.dispatcher = dispatcher;
    }

    public LoggedOperation createAccount() {
        return submitOperation(new CreateAccount(newAccountId()));
    }

    public LoggedOperation depositMoney(AccountId accountId, Decimal amount) {
        return submitOperation(new DepositTo(accountId, amount));
    }

    public LoggedOperation withdrawMoney(AccountId accountId, Decimal amount) {
        return submitOperation(new WithdrawFrom(accountId, amount));
    }

    public LoggedOperation transferMoney(AccountId fromAccountId, AccountId toAccountId, Decimal amount) {
        return submitOperation(new TransferFrom(new TransferDetail(newTransferId(), fromAccountId, toAccountId, amount)));
    }

    private LoggedOperation submitOperation(Operation operation) {
        OpLogId opLogId = operationDao.storeOperation(operation);
        List<OpLogId> unfinishedOpLogIds = operationDao.findUnfinishedOperationLogIds(opLogId.accountId);
        for (OpLogId unfinishedOpLogId : unfinishedOpLogIds) {
            LoggedOperation loggedOperation = operationDao.findLoggedOperation(unfinishedOpLogId).get();
            dispatcher.dispatchOperation(loggedOperation);
            if (opLogId.equals(unfinishedOpLogId)) {
                break;
            }
        }
        return operationDao.findLoggedOperation(opLogId).get();
    }
}
