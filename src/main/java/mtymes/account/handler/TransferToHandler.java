package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferTo;

import java.util.Optional;

public class TransferToHandler extends BaseOperationHandler<TransferTo>{

    public TransferToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(SeqId seqId, TransferTo request) {
        TransferDetail detail = request.detail;
        Optional<Account> optionalToAccount = loadAccount(detail.toAccountId);
        if (!optionalToAccount.isPresent()) {
            markAsFailure(seqId, String.format("To Account '%s' does not exist", detail.toAccountId));
            return;
        }
        depositTo(seqId, optionalToAccount.get(), detail);
    }

    private void depositTo(SeqId seqId, Account account, TransferDetail detail) {
        SeqId lastAppliedId = account.lastAppliedOpSeqId;

        if (lastAppliedId.isBefore(seqId)) {
            Decimal newBalance = account.balance.plus(detail.amount);
            accountDao.updateBalance(detail.toAccountId, newBalance, lastAppliedId, seqId);
            markAsSuccess(seqId);
        } else if (lastAppliedId.equals(seqId)) {
            markAsSuccess(seqId);
        }
    }
}
