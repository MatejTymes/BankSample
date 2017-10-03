package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.DepositTo;
import mtymes.account.domain.operation.SeqId;

import java.util.Optional;

import static java.lang.String.format;

public class DepositToHandler extends BaseOperationHandler<DepositTo> {

    public DepositToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    public void handleOperation(SeqId seqId, DepositTo operation) {
        Optional<Account> optionalAccount = loadAccount(operation.accountId);
        if (optionalAccount.isPresent()) {
            depositMoney(seqId, optionalAccount.get(), operation);
        } else {
            markOperationAsRejected(operation.operationId, format("Account '%s' does not exist", operation.accountId));
        }
    }

    private void depositMoney(SeqId seqId, Account account, DepositTo operation) {
        if (seqId.canApplyAfter(account.version)) {
            Decimal newBalance = account.balance.plus(operation.amount);
            accountDao.updateBalance(account.accountId, newBalance, account.version, seqId);
            markOperationAsApplied(operation.operationId);
        } else if (seqId.isCurrentlyApplied(account.version)) {
            markOperationAsApplied(operation.operationId);
        }
    }
}
