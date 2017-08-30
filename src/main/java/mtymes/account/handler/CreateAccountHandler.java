package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.Version;

import java.util.Optional;

import static java.lang.String.format;

public class CreateAccountHandler extends BaseOperationHandler<CreateAccount> {

    public CreateAccountHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(OpLogId opLogId, CreateAccount request) {
        AccountId accountId = request.accountId;

        boolean success = accountDao.createAccount(accountId, opLogId.version);
        if (success) {
            markAsSuccess(opLogId);
        } else {
            Optional<Version> optionalVersion = loadAccountVersion(accountId);
            if (!optionalVersion.isPresent()) {
                markAsFailure(opLogId, format("Failed to create Account '%s' and load its version", accountId));
            } else {
                Version accountVersion = optionalVersion.get();
                if (accountVersion.isBefore(opLogId.version)) {
                    markAsFailure(opLogId, format("Account '%s' already exists", accountId));
                } else if (accountVersion.equals(opLogId.version)) {
                    markAsSuccess(opLogId);
                }
            }
        }
    }
}
