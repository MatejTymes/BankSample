package mtymes.account.domain.request;

import javafixes.object.DataObject;
import mtymes.account.domain.account.AccountId;

import java.util.Set;

public abstract class Request extends DataObject {

    public abstract Set<AccountId> affectedAccountIds();
}
