package mtymes.account.domain.operation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import mtymes.account.domain.account.AccountId;

import static com.google.common.base.Preconditions.checkNotNull;

public class TransferFrom extends Operation {

    public final OperationId toPartOperationId;
    @JsonUnwrapped
    public final TransferDetail detail;

    public TransferFrom(OperationId operationId, OperationId toPartOperationId, TransferDetail detail) {
        super(operationId);

        checkNotNull(toPartOperationId, "toPartOperationId can't be null");
        checkNotNull(detail, "detail can't be null");

        this.toPartOperationId = toPartOperationId;
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
