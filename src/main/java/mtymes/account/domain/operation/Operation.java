package mtymes.account.domain.operation;

import javafixes.object.DataObject;
import mtymes.account.domain.account.AccountId;

import java.util.Set;

public abstract class Operation extends DataObject {

    public String type() {
        return getClass().getSimpleName();
    }

    public abstract Set<AccountId> affectedAccountIds();
}
