package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferTo;

import java.util.Optional;

import static java.lang.String.format;

public class TransferToHandler extends BaseOperationHandler<TransferTo>{

    public TransferToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    public void handleOperation(OpLogId opLogId, TransferTo operation) {
        TransferDetail detail = operation.detail;

        Optional<Account> optionalToAccount = loadAccount(detail.toAccountId);
        if (optionalToAccount.isPresent()) {
            depositMoney(opLogId, optionalToAccount.get(), detail);
        } else {
            markOperationAsRejected(opLogId, format("To Account '%s' does not exist", detail.toAccountId));
        }
    }

    private void depositMoney(OpLogId opLogId, Account account, TransferDetail detail) {
        if (opLogId.canApplyOperationTo(account)) {
            Decimal newBalance = account.balance.plus(detail.amount);
            accountDao.updateBalance(detail.toAccountId, newBalance, account.version, opLogId.seqId);
            markOperationAsApplied(opLogId);
        } else if (opLogId.isOperationCurrentlyAppliedTo(account)) {
            markOperationAsApplied(opLogId);
        }
    }
}
