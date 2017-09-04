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

    @Override
    public void handleOperation(OpLogId opLogId, WithdrawFrom operation) {
        AccountId accountId = operation.accountId;

        Optional<Account> optionalAccount = loadAccount(accountId);
        if (optionalAccount.isPresent()) {
            withdrawMoney(opLogId, optionalAccount.get(), operation);
        } else {
            markOperationAsRejected(opLogId, format("Account '%s' does not exist", accountId));
        }
    }

    private void withdrawMoney(OpLogId opLogId, Account account, WithdrawFrom operation) {
        if (opLogId.canApplyOperationTo(account)) {
            Decimal newBalance = account.balance.minus(operation.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markOperationAsRejected(opLogId, format("Insufficient funds on account '%s'", account.accountId));
            } else {
                accountDao.updateBalance(account.accountId, newBalance, account.version, opLogId.seqId);
                markOperationAsApplied(opLogId);
            }
        } else if (opLogId.isOperationCurrentlyAppliedTo(account)) {
            markOperationAsApplied(opLogId);
        }
    }
}
