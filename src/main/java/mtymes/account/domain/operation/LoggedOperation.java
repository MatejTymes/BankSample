package mtymes.account.domain.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import javafixes.object.DataObject;

import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

// todo: rename to PersistedOperation
public class LoggedOperation extends DataObject {

    public final Operation operation;
    @JsonInclude(value= NON_ABSENT, content= NON_EMPTY)
    public final Optional<FinalState> finalState;
    @JsonInclude(value= NON_ABSENT, content= NON_EMPTY)
    public final Optional<String> description;

    public LoggedOperation(Operation operation, Optional<FinalState> finalState, Optional<String> description) {
        checkNotNull(operation, "operation can't be null");
        checkNotNull(finalState, "finalState can't be null - use Optional.empty() instead");
        if (finalState.isPresent() && finalState.get() == FinalState.Rejected) {
            checkArgument(description != null, "Rejected Operation must have description");
            checkArgument(description.isPresent(), "Rejected Operation must have description");
        } else {
            checkNotNull(description, "description can't be null - use Optional.empty() instead");
            checkArgument(!description.isPresent(), "only Rejected Operation can have description");
        }

        this.operation = operation;
        this.finalState = finalState;
        this.description = description;
    }

    public boolean isFinished() {
        return finalState.isPresent();
    }
}
