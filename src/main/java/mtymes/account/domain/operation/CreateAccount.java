package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static javafixes.common.CollectionUtil.newSet;

public class CreateAccount extends Operation {

    public final AccountId accountId;

    public CreateAccount(AccountId accountId) {
        // todo: check conditions
        checkNotNull(accountId, "accountId can't be null");

        this.accountId = accountId;
    }

    // todo: test this
    @Override
    public Set<AccountId> affectedAccountIds() {
        return newSet(accountId);
    }
}
