package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.OperationId;

import static java.lang.String.format;
import static mtymes.account.handler.BaseOperationHandler.Progress.OlderOperationApplied;
import static mtymes.account.handler.BaseOperationHandler.Progress.ThisOperationApplied;

public class CreateAccountHandler extends BaseOperationHandler<CreateAccount> {

    public CreateAccountHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    // todo: test that can be run concurrently
    @Override
    public void handleRequest(OperationId operationId, CreateAccount request) {
        AccountId accountId = request.accountId;

        boolean success = accountDao.createAccount(accountId, operationId);
        if (success) {
            markAsSuccess(operationId);
        } else {
            Progress progress = checkProgress(accountId, operationId);
            if (progress == OlderOperationApplied) {
                markAsFailure(operationId, format("Account '%s' already exists", accountId));
            } else if (progress == ThisOperationApplied) {
                markAsSuccess(operationId);
            }
        }
    }
}
