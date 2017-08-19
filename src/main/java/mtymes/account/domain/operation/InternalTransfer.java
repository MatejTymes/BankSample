package mtymes.account.domain.operation;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javafixes.common.CollectionUtil.newSet;

public class InternalTransfer extends Operation {

    public final AccountId fromAccountId;
    public final AccountId toAccountId;
    public final Decimal amount;

    public InternalTransfer(AccountId fromAccountId, AccountId toAccountId, Decimal amount) {
        checkNotNull(fromAccountId, "fromAccountId can't be null");
        checkNotNull(toAccountId, "toAccountId can't be null");
        checkNotNull(amount, "amount can't be null");
        checkArgument(amount.compareTo(Decimal.ZERO) > 0, "amount must be a positive value");

        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    @Override
    public Set<AccountId> affectedAccountIds() {
        return newSet(fromAccountId, toAccountId);
    }

    @Override
    public <T> T apply(OperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
