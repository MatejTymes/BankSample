package mtymes.account.domain.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import javafixes.object.DataObject;

import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class LoggedOperation extends DataObject {

    public final OpLogId opLogId;
    public final Operation operation;
    @JsonInclude(value= NON_ABSENT, content= NON_EMPTY)
    public final Optional<FinalState> finalState;
    @JsonInclude(value= NON_ABSENT, content= NON_EMPTY)
    public final Optional<String> description;

    public LoggedOperation(OpLogId opLogId, Operation operation, Optional<FinalState> finalState, Optional<String> description) {
        checkNotNull(opLogId, "opLogId can't be null");
        checkNotNull(operation, "operation can't be null");
        checkNotNull(finalState, "finalState can't be null - use Optional.empty() instead");
        if (finalState.isPresent() && finalState.get() == FinalState.Failure) {
            checkArgument(description != null, "Failed Operation must have description");
            checkArgument(description.isPresent(), "Failed Operation must have description");
        } else {
            checkNotNull(description, "description can't be null - use Optional.empty() instead");
            checkArgument(!description.isPresent(), "only Failed Operation can have description");
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
