package mtymes.account.domain.operation;

import com.fasterxml.jackson.annotation.JsonGetter;
import javafixes.object.DataObject;
import mtymes.account.domain.account.AccountId;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Operation extends DataObject {

    public final OperationId operationId;

    public Operation(OperationId operationId) {
        checkNotNull(operationId, "operationId can't be null");

        this.operationId = operationId;
    }

    @JsonGetter
    public String type() {
        return getClass().getSimpleName();
    }

    public abstract AccountId affectedAccountId();

    public abstract <T> T apply(OperationVisitor<T> visitor);
}
