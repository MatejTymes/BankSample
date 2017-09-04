package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.DepositTo;
import mtymes.account.domain.operation.OpLogId;

import java.util.Optional;

import static java.lang.String.format;

public class DepositToHandler extends BaseOperationHandler<DepositTo> {

    public DepositToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    public void handleOperation(OpLogId opLogId, DepositTo operation) {
        Optional<Account> optionalAccount = loadAccount(operation.accountId);
        if (optionalAccount.isPresent()) {
            depositMoney(opLogId, optionalAccount.get(), operation);
        } else {
            markOperationAsRejected(opLogId, format("Account '%s' does not exist", operation.accountId));
        }
    }

    private void depositMoney(OpLogId opLogId, Account account, DepositTo operation) {
        if (opLogId.canApplyOperationTo(account)) {
            Decimal newBalance = account.balance.plus(operation.amount);
            accountDao.updateBalance(account.accountId, newBalance, account.version, opLogId.seqId);
            markOperationAsApplied(opLogId);
        } else if (opLogId.isOperationCurrentlyAppliedTo(account)) {
            markOperationAsApplied(opLogId);
        }
    }
}
