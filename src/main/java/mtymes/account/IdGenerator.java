package mtymes.account;

import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.TransferId;

public class IdGenerator {

    public AccountId nextAccountId() {
        return AccountId.newAccountId();
    }

    public TransferId nextTransferId() {
        return TransferId.newTransferId();
    }
}
