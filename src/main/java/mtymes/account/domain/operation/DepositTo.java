package mtymes.account.domain.operation;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class DepositTo extends Operation {

    public final AccountId accountId;
    public final Decimal amount;

    public DepositTo(OperationId operationId, AccountId accountId, Decimal amount) {
        super(operationId);

        checkNotNull(accountId, "accountId can't be null");
        checkNotNull(amount, "amount can't be null");
        checkArgument(amount.compareTo(Decimal.ZERO) > 0, "amount must be a positive value");

        this.accountId = accountId;
        this.amount = amount;
    }

    @Override
    public AccountId affectedAccountId() {
        return accountId;
    }

    @Override
    public <T> T apply(OperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
