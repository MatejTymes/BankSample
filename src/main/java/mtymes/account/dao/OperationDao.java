package mtymes.account.dao;

import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.PersistedOperation;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.exception.DuplicateOperationException;

import java.util.Optional;

public interface OperationDao {

    SeqId storeOperation(Operation operation) throws DuplicateOperationException;

    boolean markAsSuccessful(SeqId seqId);

    boolean markAsFailed(SeqId seqId, String description);

    Optional<PersistedOperation> findOperation(SeqId seqId);
}
