package mtymes.domain.account;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;

import static mtymes.test.Random.*;

public class AccountBuilder {

    private AccountId accountId = randomAccountId();
    private Decimal balance = randomAmount();
    private SeqId version = randomSeqId();

    public static AccountBuilder accountBuilder() {
        return new AccountBuilder();
    }

    public Account build() {
        return new Account(accountId, balance, version);
    }

    public AccountBuilder accountId(AccountId accountId) {
        this.accountId = accountId;
        return this;
    }

    public AccountBuilder balance(Decimal balance) {
        this.balance = balance;
        return this;
    }

    public AccountBuilder version(SeqId version) {
        this.version = version;
        return this;
    }
}
