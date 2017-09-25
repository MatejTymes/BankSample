package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;

import static com.google.common.base.Preconditions.checkNotNull;

public class CreateAccount extends Operation {

    public final OperationId operationId;
    public final AccountId accountId;

    public CreateAccount(OperationId operationId, AccountId accountId) {
        // todo: test this
        checkNotNull(operationId, "operationId can't be null");
        checkNotNull(accountId, "accountId can't be null");

        this.operationId = operationId;
        this.accountId = accountId;
    }

    @Override
    public AccountId affectedAccountId() {
        return accountId;
    }

    @Override
    public <T> T apply(OperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
