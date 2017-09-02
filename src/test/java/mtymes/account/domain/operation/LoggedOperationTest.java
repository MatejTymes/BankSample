package mtymes.account.domain.operation;

import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.randomOpLogId;
import static mtymes.test.Random.randomOperation;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LoggedOperationTest {

    @Test
    public void shouldCreateNonFinalLoggedOperation() {
        OpLogId opLogId = randomOpLogId();
        Operation operation = randomOperation();

        // When
        LoggedOperation loggedOperation = new LoggedOperation(
                opLogId,
                operation,
                Optional.empty(),
                Optional.empty()
        );

        // Then
        assertThat(loggedOperation.opLogId, equalTo(opLogId));
        assertThat(loggedOperation.operation, equalTo(operation));
        assertThat(loggedOperation.finalState, isNotPresent());
        assertThat(loggedOperation.description, isNotPresent());

        assertThat(loggedOperation.isFinished(), is(false));
    }

    @Test
    public void shouldCreateSuccessfulLoggedOperation() {
        OpLogId opLogId = randomOpLogId();
        Operation operation = randomOperation();

        // When
        LoggedOperation loggedOperation = new LoggedOperation(
                opLogId,
                operation,
                Optional.of(Success),
                Optional.empty()
        );

        // Then
        assertThat(loggedOperation.opLogId, equalTo(opLogId));
        assertThat(loggedOperation.operation, equalTo(operation));
        assertThat(loggedOperation.finalState, isPresentAndEqualTo(Success));
        assertThat(loggedOperation.description, isNotPresent());

        assertThat(loggedOperation.isFinished(), is(true));
    }

    @Test
    public void shouldCreateFailedLoggedOperation() {
        OpLogId opLogId = randomOpLogId();
        Operation operation = randomOperation();
        String description = "Some failure description";

        // When
        LoggedOperation loggedOperation = new LoggedOperation(
                opLogId,
                operation,
                Optional.of(Failure),
                Optional.of(description)
        );

        // Then
        assertThat(loggedOperation.opLogId, equalTo(opLogId));
        assertThat(loggedOperation.operation, equalTo(operation));
        assertThat(loggedOperation.finalState, isPresentAndEqualTo(Failure));
        assertThat(loggedOperation.description, isPresentAndEqualTo(description));

        assertThat(loggedOperation.isFinished(), is(true));
    }
}