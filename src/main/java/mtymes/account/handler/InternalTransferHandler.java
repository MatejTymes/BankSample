package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.InternalTransfer;
import mtymes.account.domain.operation.OperationId;

import java.util.Optional;

import static java.lang.String.format;

public class InternalTransferHandler extends BaseOperationHandler<InternalTransfer> {

    public InternalTransferHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(OperationId operationId, InternalTransfer request) {
        Optional<Account> optionalFromAccount = accountDao.findAccount(request.fromAccountId);
        if (!optionalFromAccount.isPresent()) {
            markAsFailure(operationId, String.format("Account '%s' does not exist", request.fromAccountId));
            return;
        }
        Optional<Account> optionalToAccount = accountDao.findAccount(request.toAccountId);
        if (!optionalToAccount.isPresent()) {
            markAsFailure(operationId, String.format("Account '%s' does not exist", request.toAccountId));
            return;
        }

        boolean success = withdrawMoney(operationId, optionalFromAccount.get(), request);
        if (success) {
            depositMoney(operationId, optionalToAccount.get(), request);
        }
    }

    private boolean withdrawMoney(OperationId operationId, Account account, InternalTransfer request) {
        OperationId lastAppliedId = account.lastAppliedOpId;

        if (lastAppliedId.isBefore(operationId)) {
            Decimal newBalance = account.balance.minus(request.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markAsFailure(operationId, format("Insufficient funds on account '%s'", request.fromAccountId));
                return false;
            } else {
                accountDao.updateBalance(request.fromAccountId, newBalance, lastAppliedId, operationId);
                return true;
            }
        } else {
            return lastAppliedId.equals(operationId);
        }
    }

    private void depositMoney(OperationId operationId, Account account, InternalTransfer request) {
        OperationId lastAppliedId = account.lastAppliedOpId;

        if (lastAppliedId.isBefore(operationId)) {
            Decimal newBalance = account.balance.plus(request.amount);
            accountDao.updateBalance(request.toAccountId, newBalance, lastAppliedId, operationId);
            markAsSuccess(operationId);
        } else if (lastAppliedId.equals(operationId)) {
            markAsSuccess(operationId);
        }
    }
}
