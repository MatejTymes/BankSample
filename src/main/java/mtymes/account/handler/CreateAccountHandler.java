package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.SeqId;

import java.util.Optional;

import static java.lang.String.format;

public class CreateAccountHandler extends BaseOperationHandler<CreateAccount> {

    public CreateAccountHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    public void handleOperation(SeqId seqId, CreateAccount operation) {
        boolean success = accountDao.createAccount(operation.accountId, seqId);
        if (success) {
            markOperationAsApplied(operation.operationId);
        } else {
            onAccountNotCreated(seqId, operation);
        }
    }

    private void onAccountNotCreated(SeqId seqId, CreateAccount operation) {
        Optional<SeqId> optionalVersion = loadAccountVersion(operation.accountId);
        if (!optionalVersion.isPresent()) {
            markOperationAsRejected(operation.operationId, format("Failed to create Account '%s'", operation.accountId));
        } else {
            SeqId accountVersion = optionalVersion.get();
            if (seqId.canApplyAfter(accountVersion)) {
                markOperationAsRejected(operation.operationId, format("Account '%s' already exists", operation.accountId));
            } else if (seqId.isCurrentlyApplied(accountVersion)) {
                markOperationAsApplied(operation.operationId);
            }
        }
    }
}
