package mtymes.account.domain.operation;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javafixes.common.CollectionUtil.newSet;

public class WithdrawMoney extends Operation {

    public final AccountId accountId;
    public final Decimal amount;

    public WithdrawMoney(AccountId accountId, Decimal amount) {
        checkNotNull(accountId, "accountId can't be null");
        checkNotNull(amount, "amount can't be null");
        checkArgument(amount.compareTo(Decimal.ZERO) > 0, "amount must be a positive value");

        this.accountId = accountId;
        this.amount = amount;
    }

    @Override
    public Set<AccountId> affectedAccountIds() {
        return newSet(accountId);
    }

    @Override
    public <T> T apply(OperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
