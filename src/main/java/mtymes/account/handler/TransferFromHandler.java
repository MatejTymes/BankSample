package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OpLogDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferFrom;
import mtymes.account.domain.operation.TransferTo;
import mtymes.account.exception.DuplicateItemException;
import mtymes.common.util.SetQueue;

import java.util.Optional;

import static java.lang.String.format;

public class TransferFromHandler extends BaseOperationHandler<TransferFrom> {

    private final OpLogDao opLogDao;
    private final SetQueue<AccountId> workQueue;

    public TransferFromHandler(AccountDao accountDao, OperationDao operationDao, OpLogDao opLogDao, SetQueue<AccountId> workQueue) {
        super(accountDao, operationDao);
        this.opLogDao = opLogDao;
        this.workQueue = workQueue;
    }

    @Override
    public void handleOperation(SeqId seqId, TransferFrom operation) {
        TransferDetail detail = operation.detail;
        Optional<Account> optionalFromAccount = loadAccount(detail.fromAccountId);
        if (!optionalFromAccount.isPresent()) {
            markOperationAsRejected(operation.operationId, format("From Account '%s' does not exist", detail.fromAccountId));
            return;
        }
        Optional<Account> optionalToAccount = loadAccount(detail.toAccountId);
        if (!optionalToAccount.isPresent()) {
            markOperationAsRejected(operation.operationId, format("To Account '%s' does not exist", detail.toAccountId));
            return;
        }

        boolean success = withdrawMoney(seqId, optionalFromAccount.get(), operation);
        if (success) {
            submitOperationTransferTo(operation);
        }
    }

    private boolean withdrawMoney(SeqId seqId, Account account, TransferFrom operation) {
        TransferDetail detail = operation.detail;
        if (seqId.canApplyAfter(account.version)) {
            Decimal newBalance = account.balance.minus(detail.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markOperationAsRejected(operation.operationId, format("Insufficient funds on account '%s'", detail.fromAccountId));
                return false;
            } else {
                accountDao.updateBalance(detail.fromAccountId, newBalance, account.version, seqId);
                return true;
            }
        } else {
            return seqId.isCurrentlyApplied(account.version);
        }
    }

    private void submitOperationTransferTo(TransferFrom operation) {
        TransferDetail detail = operation.detail;
        try {
            operationDao.storeOperation(new TransferTo(operation.toPartOperationId, detail));
        } catch (DuplicateItemException e) {
            // do nothing - another concurrent thread already submitted it
        }
        try {
            opLogDao.registerOperationId(detail.toAccountId, operation.toPartOperationId);
        } catch (DuplicateItemException e) {
            // do nothing - another concurrent thread already submitted it
        }
        workQueue.add(detail.toAccountId);
        markOperationAsApplied(operation.operationId);
    }
}
