package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositTo;
import mtymes.account.domain.operation.OpLogId;

import java.util.Optional;

import static java.lang.String.format;

public class DepositToHandler extends BaseOperationHandler<DepositTo> {

    public DepositToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(OpLogId opLogId, DepositTo request) {
        AccountId accountId = request.accountId;

        Optional<Account> optionalAccount = loadAccount(accountId);
        if (!optionalAccount.isPresent()) {
            markAsFailure(opLogId, format("Account '%s' does not exist", accountId));
        } else {
            depositMoney(opLogId, optionalAccount.get(), request);
        }
    }

    private void depositMoney(OpLogId opLogId, Account account, DepositTo request) {
        if (account.version.isBefore(opLogId.version)) {
            Decimal newBalance = account.balance.plus(request.amount);
            accountDao.updateBalance(account.accountId, newBalance, account.version, opLogId.version);
            markAsSuccess(opLogId);
        } else if (account.version.equals(opLogId.version)) {
            markAsSuccess(opLogId);
        }
    }
}
