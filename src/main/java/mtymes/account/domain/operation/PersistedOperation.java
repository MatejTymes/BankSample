package mtymes.account.domain.operation;

import javafixes.object.DataObject;

import java.util.Optional;

import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;

public class PersistedOperation extends DataObject {

    public final SeqId seqId;
    public final Operation operation;
    public final Optional<FinalState> finalState;
    public final Optional<String> description;

    public PersistedOperation(SeqId seqId, Operation operation, Optional<FinalState> finalState, Optional<String> description) {
        this.seqId = seqId;
        this.operation = operation;
        this.finalState = finalState;
        this.description = description;
    }

    public static PersistedOperation newOperation(SeqId seqId, Operation operation) {
        return new PersistedOperation(seqId, operation, Optional.empty(), Optional.empty());
    }

    public static PersistedOperation successfulOperation(SeqId seqId, Operation operation) {
        return new PersistedOperation(seqId, operation, Optional.of(Success), Optional.empty());
    }

    public static PersistedOperation failedOperation(SeqId seqId, Operation operation, String description) {
        return new PersistedOperation(seqId, operation, Optional.of(Failure), Optional.of(description));
    }
}
