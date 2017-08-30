package mtymes.account.domain.account;

import javafixes.math.Decimal;
import javafixes.object.DataObject;

import static com.google.common.base.Preconditions.checkNotNull;

public class Account extends DataObject {

    public final AccountId accountId;
    public final Decimal balance;
    public final Version version;

    public Account(AccountId accountId, Decimal balance, Version version) {
        checkNotNull(accountId, "accountId can't be null");
        checkNotNull(balance, "balance can't be null");
        checkNotNull(version, "version can't be null");

        this.accountId = accountId;
        this.balance = balance;
        this.version = version;
    }
}
