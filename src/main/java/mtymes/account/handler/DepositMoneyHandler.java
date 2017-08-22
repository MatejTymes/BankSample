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
    // todo: test that can be run concurrently
    @Override
    public void handleOperation(OperationId operationId, DepositMoney request) {
        AccountId accountId = request.accountId;

        Optional<Account> optionalAccount = accountDao.findAccount(accountId);
        if (!optionalAccount.isPresent()) {
            markAsFailure(operationId, String.format("Account '%s' does not exist", accountId));
        } else {
            Account account = optionalAccount.get();
            updateAccountBalance(operationId, account, calculateNewBalance(account, request.amount));
        }
    }

    private Decimal calculateNewBalance(Account account, Decimal amountToAdd) {
        return account.balance.plus(amountToAdd);
    }

    private void updateAccountBalance(OperationId operationId, Account account, Decimal newBalance) {
        OperationId lastAppliedId = account.lastAppliedOpId;
        if (lastAppliedId.isBefore(operationId)) {
            accountDao.updateBalance(account.accountId, newBalance, lastAppliedId, operationId);
            markAsSuccess(operationId);
        } else if (lastAppliedId.equals(operationId)) {
            markAsSuccess(operationId);
        }
    }
}
