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

public class WithdrawFromHandler extends BaseAccountHandler<WithdrawFrom> {

    public WithdrawFromHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    protected AccountId extractAccountId(WithdrawFrom operation) {
        return operation.accountId;
    }

    @Override
    protected void applyOperation(Account account, SeqId seqId, WithdrawFrom operation) {
        Decimal newBalance = account.balance.minus(operation.amount);
        if (newBalance.compareTo(Decimal.ZERO) < 0) {
            markOperationAsRejected(operation.operationId, format("Insufficient funds on account '%s'", account.accountId));
        } else {
            accountDao.updateBalance(account.accountId, newBalance, account.version, seqId);
            markOperationAsApplied(operation.operationId);
        }
    }
}
