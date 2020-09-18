package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.SeqId;

import java.util.Optional;

import static java.lang.String.format;

public abstract class BaseAccountHandler<T extends Operation> extends BaseOperationHandler<T> {

    protected BaseAccountHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    public void handleOperation(SeqId seqId, T operation) {
        AccountId accountId = extractAccountId(operation);
        Optional<Account> optionalAccount = loadAccount(accountId);
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            if (seqId.canApplyAfter(account.version)) {

                applyOperation(account, seqId, operation);

            } else if (seqId.isCurrentlyApplied(account.version)) {
                markOperationAsApplied(operation.operationId);
            }
        } else {
            markOperationAsRejected(operation.operationId, format(logAsAccountName() + " '%s' does not exist", accountId));
        }
    }

    protected String logAsAccountName() {
        return "Account";
    }

    protected abstract AccountId extractAccountId(T operation);

    protected abstract void applyOperation(Account account, SeqId seqId, T operation);
}
