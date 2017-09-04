package mtymes.account.domain.operation;

import javafixes.object.DataObject;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;

import static com.google.common.base.Preconditions.checkNotNull;

public class OpLogId extends DataObject {

    public final AccountId accountId;
    public final Version seqId;

    public OpLogId(AccountId accountId, Version seqId) {
        checkNotNull(accountId, "accountId can't be null");
        checkNotNull(seqId, "seqId can't be null");

        this.accountId = accountId;
        this.seqId = seqId;
    }

    public static OpLogId opLogId(AccountId accountId, Version seqId) {
        return new OpLogId(accountId, seqId);
    }
}
