package mtymes.account.domain.operation;

import javafixes.object.DataObject;
import mtymes.account.domain.account.AccountId;

public abstract class Operation extends DataObject {

    public String type() {
        return getClass().getSimpleName();
    }

    public abstract AccountId affectedAccountId();

    public abstract <T> T apply(OperationVisitor<T> visitor);
}
