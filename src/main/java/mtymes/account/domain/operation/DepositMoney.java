package mtymes.account.domain.operation;

import com.google.common.base.Preconditions;
import mtymes.account.domain.account.AccountId;

import java.math.BigDecimal;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javafixes.common.CollectionUtil.newSet;

public class DepositMoney extends Operation {

    public final AccountId accountId;
    public final BigDecimal amount;

    public DepositMoney(AccountId accountId, BigDecimal amount) {
        // todo: check conditions
        checkNotNull(accountId, "accountId can't be null");
        checkNotNull(amount, "amount can't be null");
        checkArgument(amount.compareTo(BigDecimal.ZERO) > 0, "amount must be a positive value");

        this.accountId = accountId;
        this.amount = amount;
    }

    // todo: test this
    @Override
    public Set<AccountId> affectedAccountIds() {
        return newSet(accountId);
    }

    // todo: test this
    @Override
    public <T> T apply(OperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
