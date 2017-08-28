package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static javafixes.common.CollectionUtil.newSet;

public class TransferMoneyTo extends Operation {

    public final TransferDetail detail;

    public TransferMoneyTo(TransferDetail detail) {
        checkNotNull(detail, "detail can't be null");
        this.detail = detail;
    }

    @Override
    public Set<AccountId> affectedAccountIds() {
        return newSet(detail.toAccountId);
    }

    @Override
    public <T> T apply(OperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
