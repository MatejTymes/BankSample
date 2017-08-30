package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.WithdrawFrom;

import java.util.Optional;

import static java.lang.String.format;

public class WithdrawFromHandler extends BaseOperationHandler<WithdrawFrom> {

    public WithdrawFromHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(OpLogId opLogId, WithdrawFrom request) {
        AccountId accountId = request.accountId;

        Optional<Account> optionalAccount = loadAccount(accountId);
        if (!optionalAccount.isPresent()) {
            markAsFailure(opLogId, String.format("Account '%s' does not exist", accountId));
        } else {
            withdrawMoney(opLogId, optionalAccount.get(), request);
        }
    }

    private void withdrawMoney(OpLogId opLogId, Account account, WithdrawFrom request) {
        if (account.version.isBefore(opLogId.version)) {
            Decimal newBalance = account.balance.minus(request.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markAsFailure(opLogId, format("Insufficient funds on account '%s'", account.accountId));
            } else {
                accountDao.updateBalance(account.accountId, newBalance, account.version, opLogId.version);
                markAsSuccess(opLogId);
            }
        } else if (account.version.equals(opLogId.version)) {
            markAsSuccess(opLogId);
        }
    }
}
