package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.WithdrawMoney;

import java.util.Optional;

import static java.lang.String.format;

public class WithdrawMoneyHandler extends BaseOperationHandler<WithdrawMoney> {

    public WithdrawMoneyHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    // todo: test that can be run concurrently
    @Override
    public void handleOperation(OperationId operationId, WithdrawMoney request) {
        AccountId accountId = request.accountId;

        Optional<Account> optionalAccount = accountDao.findAccount(accountId);
        if (!optionalAccount.isPresent()) {
            markAsFailure(operationId, String.format("Account '%s' does not exist", accountId));
        } else {
            Account account = optionalAccount.get();

            OperationId lastAppliedId = account.lastAppliedOpId;
            if (lastAppliedId.isBefore(operationId)) {
                Decimal newBalance = calculateNewBalance(account, request.amount);
                if (newBalance.compareTo(Decimal.ZERO) < 0) {
                    markAsFailure(operationId, format("Insufficient funds on account '%s'", accountId));
                } else {
                    accountDao.updateBalance(account.accountId, newBalance, lastAppliedId, operationId);
                    markAsSuccess(operationId);
                }
            } else if (lastAppliedId.equals(operationId)) {
                markAsSuccess(operationId);
            }

        }
    }

    private Decimal calculateNewBalance(Account account, Decimal amountToSubtract) {
        return account.balance.minus(amountToSubtract);
    }
}
