package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.WithdrawMoney;

import static java.lang.String.format;

public class WithdrawMoneyHandler extends BaseOperationHandler<WithdrawMoney> {

    public WithdrawMoneyHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test
    // todo: test that any dao interaction can fail
    // todo: test that can be run concurrently
    @Override
    void handleRequest(OperationId operationId, WithdrawMoney request) {
        AccountId accountId = request.accountId;

        Account account = loadAccount(accountId);
        OperationId lastAppliedId = account.lastAppliedOpId;
        if (lastAppliedId.isBefore(operationId)) {
            Decimal newBalance = account.balance.minus(request.amount);
            if (newBalance.compareTo(Decimal.ZERO) >= 0) {
                accountDao.updateBalance(accountId, newBalance, lastAppliedId, operationId);
                markAsSuccess(operationId);
            } else {
                markAsFailure(operationId, format("Insufficient funds on account '%s'", accountId));
            }
        } else if (lastAppliedId.equals(operationId)) {
            markAsSuccess(operationId);
        }
    }
}
