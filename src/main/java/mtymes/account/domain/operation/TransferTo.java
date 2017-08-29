package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;

import static com.google.common.base.Preconditions.checkNotNull;

public class TransferTo extends Operation {

    public final TransferDetail detail;

    public TransferTo(TransferDetail detail) {
        checkNotNull(detail, "detail can't be null");
        this.detail = detail;
    }

    @Override
    public AccountId affectedAccountId() {
        return detail.toAccountId;
    }

    @Override
    public <T> T apply(OperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
