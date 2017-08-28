package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferMoneyFrom;
import mtymes.account.domain.operation.TransferMoneyTo;

import java.util.Optional;

import static java.lang.String.format;

public class TransferMoneyFromHandler extends BaseOperationHandler<TransferMoneyFrom> {

    private final ToProcessQueue toProcessQueue;

    public TransferMoneyFromHandler(AccountDao accountDao, OperationDao operationDao, ToProcessQueue toProcessQueue) {
        super(accountDao, operationDao);
        this.toProcessQueue = toProcessQueue;
    }

    // todo: test
    // todo: test that any dao interaction can fail
    // todo: test that can be run concurrently
    @Override
    public void handleOperation(SeqId seqId, TransferMoneyFrom request) {
        TransferDetail detail = request.detail;
        Optional<Account> optionalFromAccount = accountDao.findAccount(detail.fromAccountId);
        if (!optionalFromAccount.isPresent()) {
            markAsFailure(seqId, String.format("From Account '%s' does not exist", detail.fromAccountId));
            return;
        }
        Optional<Account> optionalToAccount = accountDao.findAccount(detail.toAccountId);
        if (!optionalToAccount.isPresent()) {
            markAsFailure(seqId, String.format("To Account '%s' does not exist", detail.toAccountId));
            return;
        }

        boolean success = withdrawMoney(seqId, optionalFromAccount.get(), detail);
        if (success) {
            submitTransferToOperation(seqId, detail);
        }
    }

    private boolean withdrawMoney(SeqId seqId, Account account, TransferDetail detail) {
        SeqId lastAppliedId = account.lastAppliedOpSeqId;

        if (lastAppliedId.isBefore(seqId)) {
            Decimal newBalance = account.balance.minus(detail.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markAsFailure(seqId, format("Insufficient funds on account '%s'", detail.fromAccountId));
                return false;
            } else {
                accountDao.updateBalance(detail.fromAccountId, newBalance, lastAppliedId, seqId);
                return true;
            }
        } else {
            return lastAppliedId.equals(seqId);
        }
    }

    private void submitTransferToOperation(SeqId seqId, TransferDetail detail) {
        // todo: make sure only one copy can be stored - add unique index for transactionId + accountId
        operationDao.storeOperation(new TransferMoneyTo(detail));
        toProcessQueue.add(detail.toAccountId);
        markAsSuccess(seqId);
    }
}
