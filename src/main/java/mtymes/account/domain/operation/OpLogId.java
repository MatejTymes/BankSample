package mtymes.account.domain.operation;

import javafixes.object.DataObject;
import mtymes.account.domain.account.AccountId;

import static com.google.common.base.Preconditions.checkNotNull;

public class OpLogId extends DataObject {

    public final AccountId accountId;
    public final Version version;

    public OpLogId(AccountId accountId, Version version) {
        // todo: test this
        checkNotNull(accountId, "accountId can't be null");
        checkNotNull(version, "version can't be null");

        this.accountId = accountId;
        this.version = version;
    }

    public static OpLogId opLogId(AccountId accountId, Version version) {
        return new OpLogId(accountId, version);
    }
}
