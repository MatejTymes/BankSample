package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositTo;
import mtymes.account.domain.operation.SeqId;

import java.util.Optional;

import static java.lang.String.format;

public class DepositToHandler extends BaseAccountHandler<DepositTo> {

    public DepositToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    protected AccountId extractAccountId(DepositTo operation) {
        return operation.accountId;
    }

    @Override
    protected void applyOperation(Account account, SeqId seqId, DepositTo operation) {
        Decimal newBalance = account.balance.plus(operation.amount);
        accountDao.updateBalance(account.accountId, newBalance, account.version, seqId);
        markOperationAsApplied(operation.operationId);
    }
}
