package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositMoney;
import mtymes.account.domain.operation.OperationId;

public class DepositMoneyHandler extends OperationHandler<DepositMoney> {

    public DepositMoneyHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test
    // todo: test that any dao interaction can fail
    @Override
    public void handleRequest(OperationId operationId, DepositMoney request) {
        AccountId accountId = request.accountId;

        Account account = loadAccount(accountId);
        OperationId lastAppliedId = account.lastAppliedOpId;
        if (lastAppliedId.isBefore(operationId)) {
            accountDao.updateBalance(accountId, account.balance.plus(request.amount), lastAppliedId, operationId);
            markAsSuccess(operationId);
        } else if (lastAppliedId.equals(operationId)) {
            markAsSuccess(operationId);
        }
    }
}
