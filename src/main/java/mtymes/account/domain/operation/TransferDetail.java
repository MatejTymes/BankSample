package mtymes.account.domain.operation;

import javafixes.math.Decimal;
import javafixes.object.DataObject;
import mtymes.account.domain.account.AccountId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class TransferDetail extends DataObject {

    public final AccountId fromAccountId;
    public final AccountId toAccountId;
    public final Decimal amount;

    public TransferDetail(AccountId fromAccountId, AccountId toAccountId, Decimal amount) {
        checkNotNull(fromAccountId, "fromAccountId can't be null");
        checkNotNull(toAccountId, "toAccountId can't be null");
        checkNotNull(amount, "amount can't be null");
        checkArgument(amount.compareTo(Decimal.ZERO) > 0, "amount must be a positive value");

        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }
}
