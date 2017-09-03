package mtymes.account.domain.operation;

import com.fasterxml.jackson.annotation.JsonGetter;
import javafixes.object.DataObject;
import mtymes.account.domain.account.AccountId;

public abstract class Operation extends DataObject {

    @JsonGetter
    public String type() {
        return getClass().getSimpleName();
    }

    public abstract AccountId affectedAccountId();

    public abstract <T> T apply(OperationVisitor<T> visitor);
}
