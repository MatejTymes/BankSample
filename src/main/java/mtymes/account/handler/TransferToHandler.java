package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferTo;

import java.util.Optional;

public class TransferToHandler extends BaseOperationHandler<TransferTo>{

    public TransferToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(OpLogId opLogId, TransferTo request) {
        TransferDetail detail = request.detail;

        Optional<Account> optionalToAccount = loadAccount(detail.toAccountId);
        if (!optionalToAccount.isPresent()) {
            markAsFailure(opLogId, String.format("To Account '%s' does not exist", detail.toAccountId));
        } else {
            depositTo(opLogId, optionalToAccount.get(), detail);
        }
    }

    private void depositTo(OpLogId opLogId, Account account, TransferDetail detail) {
        if (account.version.isBefore(opLogId.version)) {
            Decimal newBalance = account.balance.plus(detail.amount);
            accountDao.updateBalance(detail.toAccountId, newBalance, account.version, opLogId.version);
            markAsSuccess(opLogId);
        } else if (account.version.equals(opLogId.version)) {
            markAsSuccess(opLogId);
        }
    }
}
