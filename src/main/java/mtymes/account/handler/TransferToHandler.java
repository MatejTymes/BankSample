package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferTo;

import java.util.Optional;

import static java.lang.String.format;

public class TransferToHandler extends BaseOperationHandler<TransferTo>{

    public TransferToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    public void handleOperation(SeqId seqId, TransferTo operation) {
        TransferDetail detail = operation.detail;

        Optional<Account> optionalToAccount = loadAccount(detail.toAccountId);
        if (optionalToAccount.isPresent()) {
            depositMoney(seqId, optionalToAccount.get(), operation);
        } else {
            markOperationAsRejected(operation.operationId, format("To Account '%s' does not exist", detail.toAccountId));
        }
    }

    private void depositMoney(SeqId seqId, Account account, TransferTo operation) {
        if (seqId.canApplyAfter(account.version)) {
            TransferDetail detail = operation.detail;
            Decimal newBalance = account.balance.plus(detail.amount);
            accountDao.updateBalance(detail.toAccountId, newBalance, account.version, seqId);
            markOperationAsApplied(operation.operationId);
        } else if (seqId.isCurrentlyApplied(account.version)) {
            markOperationAsApplied(operation.operationId);
        }
    }
}
