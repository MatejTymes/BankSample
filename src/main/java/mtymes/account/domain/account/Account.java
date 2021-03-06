package mtymes.account.domain.account;

import javafixes.math.Decimal;
import javafixes.object.DataObject;
import mtymes.account.domain.operation.SeqId;

import static com.google.common.base.Preconditions.checkNotNull;

public class Account extends DataObject {

    public final AccountId accountId;
    public final Decimal balance;
    public final SeqId version;

    public Account(AccountId accountId, Decimal balance, SeqId version) {
        checkNotNull(accountId, "accountId can't be null");
        checkNotNull(balance, "balance can't be null");
        checkNotNull(version, "version can't be null");

        this.accountId = accountId;
        this.balance = balance;
        this.version = version;
    }

    @SuppressWarnings("unused")
    private Account() {
        this.accountId = null;
        this.balance = null;
        this.version = null;
    }
}
