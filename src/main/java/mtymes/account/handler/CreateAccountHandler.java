package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.SeqId;

import static java.lang.String.format;

public class CreateAccountHandler extends BaseOperationHandler<CreateAccount> {

    public CreateAccountHandler(AccountDao accountDao, OperationDao operationDao) {
        super(accountDao, operationDao);
    }

    // todo: test that any dao interaction can fail
    @Override
    public void handleOperation(SeqId seqId, CreateAccount request) {
        AccountId accountId = request.accountId;

        boolean success = accountDao.createAccount(accountId, seqId);
        if (success) {
            markAsSuccess(seqId);
        } else {
            SeqId lastSeqId = loadLastAppliedOperationId(accountId);
            if (lastSeqId.isBefore(seqId)) {
                markAsFailure(seqId, format("Account '%s' already exists", accountId));
            } else if (lastSeqId.equals(seqId)) {
                markAsSuccess(seqId);
            }
        }
    }

    private SeqId loadLastAppliedOperationId(AccountId accountId) {
        return accountDao
                .findLastAppliedOperationId(accountId)
                .orElseThrow(
                        () -> new IllegalStateException(format("Failed to load SeqId for Account '%s'", accountId))
                );
    }
}
