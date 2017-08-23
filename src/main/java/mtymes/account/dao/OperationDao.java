package mtymes.account.dao;

import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.PersistedOperation;
import mtymes.account.domain.operation.SeqId;

import java.util.Optional;

public interface OperationDao {

    SeqId storeOperation(Operation operation);

    boolean markAsSuccessful(SeqId seqId);

    boolean markAsFailed(SeqId seqId, String description);

    Optional<PersistedOperation> findOperation(SeqId seqId);
}
