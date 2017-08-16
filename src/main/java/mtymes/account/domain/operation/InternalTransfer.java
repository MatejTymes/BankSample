package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;

import java.math.BigDecimal;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javafixes.common.CollectionUtil.newSet;

public class InternalTransfer extends Operation {

    public final AccountId fromAccountId;
    public final AccountId toAccountId;
    public final BigDecimal amount;

    public InternalTransfer(AccountId fromAccountId, AccountId toAccountId, BigDecimal amount) {
        // todo: check conditions
        checkNotNull(fromAccountId, "fromAccountId can't be null");
        checkNotNull(toAccountId, "toAccountId can't be null");
        checkNotNull(amount, "amount can't be null");
        checkArgument(amount.compareTo(BigDecimal.ZERO) > 0, "amount must be a positive value");

        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    // todo: test this
    @Override
    public Set<AccountId> affectedAccountIds() {
        return newSet(fromAccountId, toAccountId);
    }

    // todo: test this
    @Override
    public <T> T accept(OperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
