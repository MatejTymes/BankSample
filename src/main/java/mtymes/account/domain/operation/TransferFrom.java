package mtymes.account.domain.operation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import mtymes.account.domain.account.AccountId;

import static com.google.common.base.Preconditions.checkNotNull;

public class TransferFrom extends Operation {

    @JsonUnwrapped
    public final TransferDetail detail;

    public TransferFrom(TransferDetail detail) {
        checkNotNull(detail, "detail can't be null");
        this.detail = detail;
    }

    @Override
    public AccountId affectedAccountId() {
        return detail.fromAccountId;
    }

    @Override
    public <T> T apply(OperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
