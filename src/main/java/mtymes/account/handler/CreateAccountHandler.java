package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;

import static java.lang.String.format;
import static mtymes.account.handler.OperationHandler.Progress.OlderOperationApplied;
import static mtymes.account.handler.OperationHandler.Progress.ThisOperationApplied;

public class CreateAccountHandler extends OperationHandler<CreateAccount> {

    public CreateAccountHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test this
    @Override
    public boolean canHandleRequest(Operation operation) {
        return operation instanceof CreateAccount;
    }

    // todo: test
    // todo: test that any dao interaction can fail
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
