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
    public void handleOperation(OpLogId opLogId, CreateAccount request) {

        boolean success = accountDao.createAccount(request.accountId, opLogId.version);
        if (success) {
            markAsSuccess(opLogId);
        } else {
            onAccountNotCreated(opLogId, request);
        }
    }

    private void onAccountNotCreated(OpLogId opLogId, CreateAccount request) {
        Optional<Version> optionalVersion = loadAccountVersion(request.accountId);
        if (!optionalVersion.isPresent()) {
            markAsFailure(opLogId, format("Failed to create Account '%s'", request.accountId));
        } else {
            Version accountVersion = optionalVersion.get();
            if (accountVersion.isBefore(opLogId.version)) {
                markAsFailure(opLogId, format("Account '%s' already exists", request.accountId));
            } else if (accountVersion.equals(opLogId.version)) {
                markAsSuccess(opLogId);
            }
        }
    }
}
