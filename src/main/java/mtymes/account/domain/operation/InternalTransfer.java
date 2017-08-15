package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;

import java.math.BigDecimal;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javafixes.common.CollectionUtil.newSet;

public class InternalTransfer extends Operation {

    public final AccountId fromAccount;
    public final AccountId toAccount;
    public final BigDecimal amount;

    public InternalTransfer(AccountId fromAccount, AccountId toAccount, BigDecimal amount) {
        // todo: check conditions
        checkNotNull(fromAccount, "fromAccount can't be null");
        checkNotNull(toAccount, "toAccount can't be null");
        checkNotNull(amount, "amount can't be null");
        checkArgument(amount.compareTo(BigDecimal.ZERO) > 0, "amount must be a positive value");

        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
    }

    // todo: test this
    @Override
    public Set<AccountId> affectedAccountIds() {
        return newSet(fromAccount, toAccount);
    }
}
