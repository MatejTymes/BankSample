package mtymes.account.domain.operation;

import javafixes.object.DataObject;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class LoggedOperation extends DataObject {

    public final OpLogId opLogId;
    public final Operation operation;
    public final Optional<FinalState> finalState;
    public final Optional<String> description;

    public LoggedOperation(OpLogId opLogId, Operation operation, Optional<FinalState> finalState, Optional<String> description) {
        // todo: test this
        checkNotNull(opLogId, "opLogId can't be null");
        checkNotNull(operation, "operation can't be null");
        checkNotNull(finalState, "finalState can't be null - use Optional.empty() instead");
        checkNotNull(description, "description can't be null - use Optional.empty() instead");
        if (finalState.isPresent()) {
            if (finalState.get() == FinalState.Success) {
                checkArgument(!description.isPresent(), "description can't have value");
            } else if (finalState.get() == FinalState.Failure) {
                checkArgument(description.isPresent(), "description must have value");
            }
        } else {
            checkArgument(!description.isPresent(), "description can't have value");
        }

        this.opLogId = opLogId;
        this.operation = operation;
        this.finalState = finalState;
        this.description = description;
    }

    public boolean isFinished() {
        return finalState.isPresent();
    }
}
