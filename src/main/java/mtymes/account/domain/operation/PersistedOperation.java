package mtymes.account.domain.operation;

import javafixes.object.DataObject;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;

public class PersistedOperation extends DataObject {

    public final OpLogId opLogId;
    public final Operation operation;
    public final Optional<FinalState> finalState;
    public final Optional<String> description;

    public PersistedOperation(OpLogId opLogId, Operation operation, Optional<FinalState> finalState, Optional<String> description) {
        // todo: test this
        checkNotNull(opLogId, "opLogId can't be null");
        checkNotNull(operation, "operation can't be null");
        checkNotNull(finalState, "finalState can't be null");
        checkNotNull(description, "description can't be null");
        if (finalState.isPresent()) {
            if (finalState.get() == FinalState.Success) {
                checkArgument(!description.isPresent(), "description can't have value");
            } else if (finalState.get() == FinalState.Failure) {
                checkArgument(description.isPresent(), "description must have value");
            }
        }

        this.opLogId = opLogId;
        this.operation = operation;
        this.finalState = finalState;
        this.description = description;
    }

    public static PersistedOperation newOperation(OpLogId opLogId, Operation operation) {
        return new PersistedOperation(opLogId, operation, Optional.empty(), Optional.empty());
    }

    public static PersistedOperation successfulOperation(OpLogId opLogId, Operation operation) {
        return new PersistedOperation(opLogId, operation, Optional.of(Success), Optional.empty());
    }

    public static PersistedOperation failedOperation(OpLogId opLogId, Operation operation, String description) {
        return new PersistedOperation(opLogId, operation, Optional.of(Failure), Optional.of(description));
    }
}
