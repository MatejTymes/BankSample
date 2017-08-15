package mtymes.account.domain.account;

import javafixes.object.DataObject;
import mtymes.account.domain.operation.OperationId;

import java.math.BigDecimal;

public class Account extends DataObject {

    public final AccountId accountId;
    public final BigDecimal balance;
    public final OperationId lastAppliedOperation;

    public Account(AccountId accountId, BigDecimal balance, OperationId lastAppliedOperation) {
        this.accountId = accountId;
        this.balance = balance;
        this.lastAppliedOperation = lastAppliedOperation;
    }
}
