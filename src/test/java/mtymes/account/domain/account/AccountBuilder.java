package mtymes.account.domain.account;

import javafixes.math.Decimal;
import mtymes.account.domain.operation.OperationId;

import static mtymes.test.Random.*;

public class AccountBuilder {


    private AccountId accountId = randomAccountId();
    private Decimal balance = randomDecimal();
    private OperationId lastAppliedOperationId = randomOperationId();

    public static AccountBuilder accountBuilder() {
        return new AccountBuilder();
    }

    public Account build() {
        return new Account(accountId, balance, lastAppliedOperationId);
    }

    public AccountBuilder accountId(AccountId accountId) {
        this.accountId = accountId;
        return this;
    }

    public AccountBuilder balance(Decimal balance) {
        this.balance = balance;
        return this;
    }

    public AccountBuilder lastAppliedOperationId(OperationId lastAppliedOperationId) {
        this.lastAppliedOperationId = lastAppliedOperationId;
        return this;
    }
}
