package mtymes.account.domain.account;

import javafixes.math.Decimal;
import javafixes.object.DataObject;
import mtymes.account.domain.operation.SeqId;

public class Account extends DataObject {

    public final AccountId accountId;
    public final Decimal balance;
    public final SeqId lastAppliedOpId;

    public Account(AccountId accountId, Decimal balance, SeqId lastAppliedOperation) {
        this.accountId = accountId;
        this.balance = balance;
        this.lastAppliedOpId = lastAppliedOperation;
    }
}
