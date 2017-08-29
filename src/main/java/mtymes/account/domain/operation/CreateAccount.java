package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;

import static com.google.common.base.Preconditions.checkNotNull;

public class CreateAccount extends Operation {

    public final AccountId accountId;

    public CreateAccount(AccountId accountId) {
        checkNotNull(accountId, "accountId can't be null");

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
