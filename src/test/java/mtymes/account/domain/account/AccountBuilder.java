package mtymes.account.domain.account;

import javafixes.math.Decimal;
import mtymes.account.domain.operation.SeqId;

import static mtymes.test.Random.*;

// todo: move into test-infrastructure
public class AccountBuilder {

    private AccountId accountId = randomAccountId();
    private Decimal balance = randomDecimal();
    private SeqId lastAppliedSeqId = randomOperationId();

    public static AccountBuilder accountBuilder() {
        return new AccountBuilder();
    }

    public Account build() {
        return new Account(accountId, balance, lastAppliedSeqId);
    }

    public AccountBuilder accountId(AccountId accountId) {
        this.accountId = accountId;
        return this;
    }

    public AccountBuilder balance(Decimal balance) {
        this.balance = balance;
        return this;
    }

    public AccountBuilder lastAppliedOperationId(SeqId lastAppliedSeqId) {
        this.lastAppliedSeqId = lastAppliedSeqId;
        return this;
    }
}
