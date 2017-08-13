package mtymes.account.domain.request;

import mtymes.account.domain.account.AccountId;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static javafixes.common.CollectionUtil.newSet;

public class CreateAccount extends Request {

    public final AccountId accountId;

    public CreateAccount(AccountId accountId) {
        checkNotNull(accountId, "accountId can't be null");
        this.accountId = accountId;
    }

    @Override
    public Set<AccountId> affectedAccountIds() {
        return newSet(accountId);
    }
}
