package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferTo;

import java.util.Optional;

import static java.lang.String.format;

public class TransferToHandler extends BaseAccountHandler<TransferTo>{

    public TransferToHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    protected AccountId extractAccountId(TransferTo operation) {
        return operation.detail.toAccountId;
    }

    @Override
    protected String logAsAccountName() {
        return "To Account";
    }

    @Override
    protected void applyOperation(Account account, SeqId seqId, TransferTo operation) {
        TransferDetail detail = operation.detail;
        Decimal newBalance = account.balance.plus(detail.amount);
        accountDao.updateBalance(detail.toAccountId, newBalance, account.version, seqId);
        markOperationAsApplied(operation.operationId);
    }
}
