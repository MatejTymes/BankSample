package mtymes.account.domain.operation;

import javafixes.object.DataObject;

import java.util.Optional;

import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;

public class PersistedOperation extends DataObject {

    public final OperationId operationId;
    public final Operation operation;
    public final Optional<FinalState> finalState;
    public final Optional<String> description;

    public PersistedOperation(OperationId operationId, Operation operation, Optional<FinalState> finalState, Optional<String> description) {
        this.operationId = operationId;
        this.operation = operation;
        this.finalState = finalState;
        this.description = description;
    }

    public static PersistedOperation newOperation(OperationId operationId, Operation operation) {
        return new PersistedOperation(operationId, operation, Optional.empty(), Optional.empty());
    }

    public static PersistedOperation successfulOperation(OperationId operationId, Operation operation) {
        return new PersistedOperation(operationId, operation, Optional.of(Success), Optional.empty());
    }

    public static PersistedOperation failedOperation(OperationId operationId, Operation operation, String description) {
        return new PersistedOperation(operationId, operation, Optional.of(Failure), Optional.of(description));
    }
}
