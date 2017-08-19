package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.InternalTransfer;
import mtymes.account.domain.operation.OperationId;

import static java.lang.String.format;

public class InternalTransferHandler extends BaseOperationHandler<InternalTransfer> {

    public InternalTransferHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test
    // todo: test that any dao interaction can fail
    // todo: test that can be run concurrently
    @Override
    public void handleRequest(OperationId operationId, InternalTransfer request) {
        Account fromAccount = loadAccount(request.fromAccountId);
        Account toAccount = loadAccount(request.toAccountId);

        boolean success = withDrawMoney(fromAccount, operationId, request);
        if (success) {
            depositMoney(toAccount, operationId, request);
        }
    }

    private boolean withDrawMoney(Account account, OperationId operationId, InternalTransfer request) {
        AccountId accountId = request.fromAccountId;

        OperationId lastAppliedId = account.lastAppliedOpId;
        if (lastAppliedId.isBefore(operationId)) {
            Decimal newBalance = account.balance.minus(request.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markAsFailure(operationId, format("Insufficient funds on account '%s'", accountId));
                return false;
            } else {
                accountDao.updateBalance(accountId, newBalance, lastAppliedId, operationId);
                return true;
            }
        } else {
            return lastAppliedId.equals(operationId);
        }
    }

    private void depositMoney(Account account, OperationId operationId, InternalTransfer request) {
        AccountId accountId = request.toAccountId;

        OperationId lastAppliedId = account.lastAppliedOpId;
        if (lastAppliedId.isBefore(operationId)) {
            accountDao.updateBalance(accountId, account.balance.plus(request.amount), lastAppliedId, operationId);
            markAsSuccess(operationId);
        } else if (lastAppliedId.equals(operationId)) {
            markAsSuccess(operationId);
        }
    }
}
