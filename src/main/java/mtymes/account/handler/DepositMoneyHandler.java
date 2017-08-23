package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositMoney;
import mtymes.account.domain.operation.OperationId;

import java.util.Optional;

public class DepositMoneyHandler extends BaseOperationHandler<DepositMoney> {

    public DepositMoneyHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(OperationId operationId, DepositMoney request) {
        AccountId accountId = request.accountId;

        Optional<Account> optionalAccount = accountDao.findAccount(accountId);
        if (!optionalAccount.isPresent()) {
            markAsFailure(operationId, String.format("Account '%s' does not exist", accountId));
        } else {
            depositMoney(operationId, optionalAccount.get(), request);
        }
    }

    private void depositMoney(OperationId operationId, Account account, DepositMoney request) {
        OperationId lastAppliedId = account.lastAppliedOpId;

        if (lastAppliedId.isBefore(operationId)) {
            Decimal newBalance = calculateNewBalance(account, request.amount);
            accountDao.updateBalance(account.accountId, newBalance, lastAppliedId, operationId);
            markAsSuccess(operationId);
        } else if (lastAppliedId.equals(operationId)) {
            markAsSuccess(operationId);
        }
    }

    private Decimal calculateNewBalance(Account account, Decimal amountToAdd) {
        return account.balance.plus(amountToAdd);
    }
}
