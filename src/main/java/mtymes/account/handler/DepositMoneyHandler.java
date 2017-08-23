package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositMoney;
import mtymes.account.domain.operation.SeqId;

import java.util.Optional;

public class DepositMoneyHandler extends BaseOperationHandler<DepositMoney> {

    public DepositMoneyHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(SeqId seqId, DepositMoney request) {
        AccountId accountId = request.accountId;

        Optional<Account> optionalAccount = accountDao.findAccount(accountId);
        if (!optionalAccount.isPresent()) {
            markAsFailure(seqId, String.format("Account '%s' does not exist", accountId));
        } else {
            depositMoney(seqId, optionalAccount.get(), request);
        }
    }

    private void depositMoney(SeqId seqId, Account account, DepositMoney request) {
        SeqId lastAppliedId = account.lastAppliedOpSeqId;

        if (lastAppliedId.isBefore(seqId)) {
            Decimal newBalance = calculateNewBalance(account, request.amount);
            accountDao.updateBalance(account.accountId, newBalance, lastAppliedId, seqId);
            markAsSuccess(seqId);
        } else if (lastAppliedId.equals(seqId)) {
            markAsSuccess(seqId);
        }
    }

    private Decimal calculateNewBalance(Account account, Decimal amountToAdd) {
        return account.balance.plus(amountToAdd);
    }
}
