package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.InternalTransfer;
import mtymes.account.domain.operation.SeqId;

import java.util.Optional;

import static java.lang.String.format;

public class InternalTransferHandler extends BaseOperationHandler<InternalTransfer> {

    public InternalTransferHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(SeqId seqId, InternalTransfer request) {
        Optional<Account> optionalFromAccount = accountDao.findAccount(request.fromAccountId);
        if (!optionalFromAccount.isPresent()) {
            markAsFailure(seqId, String.format("Account '%s' does not exist", request.fromAccountId));
            return;
        }
        Optional<Account> optionalToAccount = accountDao.findAccount(request.toAccountId);
        if (!optionalToAccount.isPresent()) {
            markAsFailure(seqId, String.format("Account '%s' does not exist", request.toAccountId));
            return;
        }

        boolean success = withdrawMoney(seqId, optionalFromAccount.get(), request);
        if (success) {
            depositMoney(seqId, optionalToAccount.get(), request);
        }
    }

    private boolean withdrawMoney(SeqId seqId, Account account, InternalTransfer request) {
        SeqId lastAppliedId = account.lastAppliedOpSeqId;

        if (lastAppliedId.isBefore(seqId)) {
            Decimal newBalance = account.balance.minus(request.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markAsFailure(seqId, format("Insufficient funds on account '%s'", request.fromAccountId));
                return false;
            } else {
                accountDao.updateBalance(request.fromAccountId, newBalance, lastAppliedId, seqId);
                return true;
            }
        } else {
            return lastAppliedId.equals(seqId);
        }
    }

    private void depositMoney(SeqId seqId, Account account, InternalTransfer request) {
        SeqId lastAppliedId = account.lastAppliedOpSeqId;

        if (lastAppliedId.isBefore(seqId)) {
            Decimal newBalance = account.balance.plus(request.amount);
            accountDao.updateBalance(request.toAccountId, newBalance, lastAppliedId, seqId);
            markAsSuccess(seqId);
        } else if (lastAppliedId.equals(seqId)) {
            markAsSuccess(seqId);
        }
    }
}
