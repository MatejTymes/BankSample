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
        if (optionalAccount.isPresent()) {
            withdrawMoney(opLogId, optionalAccount.get(), request);
        } else {
            markAsRejected(opLogId, String.format("Account '%s' does not exist", accountId));
        }
    }

    private void withdrawMoney(OpLogId opLogId, Account account, WithdrawFrom request) {
        if (canApplyOperationTo(opLogId, account)) {
            Decimal newBalance = account.balance.minus(request.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markAsRejected(opLogId, format("Insufficient funds on account '%s'", account.accountId));
            } else {
                accountDao.updateBalance(account.accountId, newBalance, account.version, opLogId.seqId);
                markAsApplied(opLogId);
            }
        } else if (isOperationCurrentlyAppliedTo(opLogId, account)) {
            markAsApplied(opLogId);
        }
    }
}
