package mtymes.account.domain.account;

import javafixes.math.Decimal;
import javafixes.object.DataObject;
import mtymes.account.domain.operation.OperationId;

public class Account extends DataObject {

    public final AccountId accountId;
    public final Decimal balance;
    public final OperationId lastAppliedOpId;

    public Account(AccountId accountId, Decimal balance, OperationId lastAppliedOperation) {
        this.accountId = accountId;
        this.balance = balance;
        this.lastAppliedOpId = lastAppliedOperation;
    }
}
