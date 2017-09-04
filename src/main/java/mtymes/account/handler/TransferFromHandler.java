package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.WorkQueue;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferFrom;
import mtymes.account.domain.operation.TransferTo;
import mtymes.account.exception.DuplicateOperationException;

import java.util.Optional;

import static java.lang.String.format;

public class TransferFromHandler extends BaseOperationHandler<TransferFrom> {

    private final WorkQueue workQueue;

    public TransferFromHandler(AccountDao accountDao, OperationDao operationDao, WorkQueue workQueue) {
        super(accountDao, operationDao);
        this.workQueue = workQueue;
    }

    @Override
    public void handleOperation(OpLogId opLogId, TransferFrom operation) {
        TransferDetail detail = operation.detail;
        Optional<Account> optionalFromAccount = loadAccount(detail.fromAccountId);
        if (!optionalFromAccount.isPresent()) {
            markOperationAsRejected(opLogId, format("From Account '%s' does not exist", detail.fromAccountId));
            return;
        }
        Optional<Account> optionalToAccount = loadAccount(detail.toAccountId);
        if (!optionalToAccount.isPresent()) {
            markOperationAsRejected(opLogId, format("To Account '%s' does not exist", detail.toAccountId));
            return;
        }

        boolean success = withdrawMoney(opLogId, optionalFromAccount.get(), detail);
        if (success) {
            submitOperationTransferTo(opLogId, detail);
        }
    }

    private boolean withdrawMoney(OpLogId opLogId, Account account, TransferDetail detail) {
        if (opLogId.canApplyOperationTo(account)) {
            Decimal newBalance = account.balance.minus(detail.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markOperationAsRejected(opLogId, format("Insufficient funds on account '%s'", detail.fromAccountId));
                return false;
            } else {
                accountDao.updateBalance(detail.fromAccountId, newBalance, account.version, opLogId.seqId);
                return true;
            }
        } else {
            return opLogId.isOperationCurrentlyAppliedTo(account);
        }
    }

    private void submitOperationTransferTo(OpLogId opLogId, TransferDetail detail) {
        try {
            operationDao.storeOperation(new TransferTo(detail));
        } catch (DuplicateOperationException e) {
            // do nothing - another concurrent thread already submitted it
        }
        workQueue.add(detail.toAccountId);
        markOperationAsApplied(opLogId);
    }
}
