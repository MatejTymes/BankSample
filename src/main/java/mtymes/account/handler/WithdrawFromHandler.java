package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.WithdrawFrom;

import java.util.Optional;

import static java.lang.String.format;

public class WithdrawFromHandler extends BaseOperationHandler<WithdrawFrom> {

    public WithdrawFromHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    public void handleOperation(SeqId seqId, WithdrawFrom operation) {
        AccountId accountId = operation.accountId;

        Optional<Account> optionalAccount = loadAccount(accountId);
        if (optionalAccount.isPresent()) {
            withdrawMoney(seqId, optionalAccount.get(), operation);
        } else {
            markOperationAsRejected(operation.operationId, format("Account '%s' does not exist", accountId));
        }
    }

    private void withdrawMoney(SeqId seqId, Account account, WithdrawFrom operation) {
        if (seqId.canApplyAfter(account.version)) {
            Decimal newBalance = account.balance.minus(operation.amount);
            if (newBalance.compareTo(Decimal.ZERO) < 0) {
                markOperationAsRejected(operation.operationId, format("Insufficient funds on account '%s'", account.accountId));
            } else {
                accountDao.updateBalance(account.accountId, newBalance, account.version, seqId);
                markOperationAsApplied(operation.operationId);
            }
        } else if (seqId.isCurrentlyApplied(account.version)) {
            markOperationAsApplied(operation.operationId);
        }
    }
}
