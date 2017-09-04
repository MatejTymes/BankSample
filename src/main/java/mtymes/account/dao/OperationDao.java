package mtymes.account.dao;

import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.exception.DuplicateOperationException;

import java.util.List;
import java.util.Optional;

public interface OperationDao {

    OpLogId storeOperation(Operation operation) throws DuplicateOperationException;

    boolean markAsApplied(OpLogId opLogId);

    boolean markAsRejected(OpLogId opLogId, String description);

    Optional<LoggedOperation> findLoggedOperation(OpLogId opLogId);

    List<OpLogId> findUnfinishedOperationLogIds(AccountId accountId);
}
