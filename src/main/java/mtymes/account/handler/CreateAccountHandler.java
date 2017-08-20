package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.OperationId;

import static java.lang.String.format;

public class CreateAccountHandler extends BaseOperationHandler<CreateAccount> {

    public CreateAccountHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    // todo: test that can be run concurrently
    @Override
    public void handleOperation(OperationId operationId, CreateAccount request) {
        AccountId accountId = request.accountId;

        boolean success = accountDao.createAccount(accountId, operationId);
        if (success) {
            markAsSuccess(operationId);
        } else {
            OperationId lastOperationId = loadLastAppliedOperationId(accountId);
            if (lastOperationId.isBefore(operationId)) {
                markAsFailure(operationId, format("Account '%s' already exists", accountId));
            } else if (lastOperationId.equals(operationId)) {
                markAsSuccess(operationId);
            }
        }
    }
}
