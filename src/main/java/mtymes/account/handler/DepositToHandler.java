package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.operation.DepositTo;
import mtymes.account.domain.operation.OpLogId;

import java.util.Optional;

import static java.lang.String.format;

public class DepositToHandler extends BaseOperationHandler<DepositTo> {

    public DepositToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    public void handleOperation(OpLogId opLogId, DepositTo request) {
        Optional<Account> optionalAccount = loadAccount(request.accountId);
        if (optionalAccount.isPresent()) {
            depositMoney(opLogId, optionalAccount.get(), request);
        } else {
            markAsRejected(opLogId, format("Account '%s' does not exist", request.accountId));
        }
    }

    private void depositMoney(OpLogId opLogId, Account account, DepositTo request) {
        if (opLogId.canApplyOperationTo(account)) {
            Decimal newBalance = account.balance.plus(request.amount);
            accountDao.updateBalance(account.accountId, newBalance, account.version, opLogId.seqId);
            markAsApplied(opLogId);
        } else if (opLogId.isOperationCurrentlyAppliedTo(account)) {
            markAsApplied(opLogId);
        }
    }
}
