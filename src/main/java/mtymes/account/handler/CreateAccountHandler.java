package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Version;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.OpLogId;

import java.util.Optional;

import static java.lang.String.format;

public class CreateAccountHandler extends BaseOperationHandler<CreateAccount> {

    public CreateAccountHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    @Override
    public void handleOperation(OpLogId opLogId, CreateAccount operation) {

        boolean success = accountDao.createAccount(operation.accountId, opLogId.seqId);
        if (success) {
            markOperationAsApplied(opLogId);
        } else {
            onAccountNotCreated(opLogId, operation);
        }
    }

    private void onAccountNotCreated(OpLogId opLogId, CreateAccount operation) {
        Optional<Version> optionalVersion = loadAccountVersion(operation.accountId);
        if (!optionalVersion.isPresent()) {
            markOperationAsRejected(opLogId, format("Failed to create Account '%s'", operation.accountId));
        } else {
            Version accountVersion = optionalVersion.get();
            if (opLogId.canApplyOperationTo(accountVersion)) {
                markOperationAsRejected(opLogId, format("Account '%s' already exists", operation.accountId));
            } else if (opLogId.isOperationCurrentlyAppliedTo(accountVersion)) {
                markOperationAsApplied(opLogId);
            }
        }
    }
}
