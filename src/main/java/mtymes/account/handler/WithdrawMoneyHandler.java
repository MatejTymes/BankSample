package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.WithdrawMoney;

import java.util.Optional;

import static java.lang.String.format;

public class WithdrawMoneyHandler extends BaseOperationHandler<WithdrawMoney> {

    public WithdrawMoneyHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(SeqId seqId, WithdrawMoney request) {
        AccountId accountId = request.accountId;

        Optional<Account> optionalAccount = loadAccount(accountId);
        if (!optionalAccount.isPresent()) {
            markAsFailure(seqId, String.format("Account '%s' does not exist", accountId));
        } else {
            withdrawMoney(seqId, optionalAccount.get(), request);
        }
    }

    private void withdrawMoney(SeqId seqId, Account account, WithdrawMoney request) {
        SeqId lastAppliedId = account.lastAppliedOpSeqId;

        if (lastAppliedId.isBefore(seqId)) {
            Decimal newBalance = calculateNewBalance(account, request.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markAsFailure(seqId, format("Insufficient funds on account '%s'", account.accountId));
            } else {
                accountDao.updateBalance(account.accountId, newBalance, lastAppliedId, seqId);
                markAsSuccess(seqId);
            }
        } else if (lastAppliedId.equals(seqId)) {
            markAsSuccess(seqId);
        }
    }

    private Decimal calculateNewBalance(Account account, Decimal amountToSubtract) {
        return account.balance.minus(amountToSubtract);
    }
}
