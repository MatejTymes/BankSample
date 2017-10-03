package mtymes.account.domain.operation;

import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.randomOperation;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LoggedOperationTest {

    @Test
    public void shouldCreateNonFinalLoggedOperation() {
        Operation operation = randomOperation();

        // When
        LoggedOperation loggedOperation = new LoggedOperation(
                operation,
                Optional.empty(),
                Optional.empty()
        );

        // Then
        assertThat(loggedOperation.operation, equalTo(operation));
        assertThat(loggedOperation.finalState, isNotPresent());
        assertThat(loggedOperation.description, isNotPresent());

        assertThat(loggedOperation.isFinished(), is(false));
    }

    @Test
    public void shouldCreateLoggedOperationInAppliedState() {
        Operation operation = randomOperation();

        // When
        LoggedOperation loggedOperation = new LoggedOperation(
                operation,
                Optional.of(Applied),
                Optional.empty()
        );

        // Then
        assertThat(loggedOperation.operation, equalTo(operation));
        assertThat(loggedOperation.finalState, isPresentAndEqualTo(Applied));
        assertThat(loggedOperation.description, isNotPresent());

        assertThat(loggedOperation.isFinished(), is(true));
    }

    @Test
    public void shouldCreateLoggedOperationInRejectedState() {
        Operation operation = randomOperation();
        String description = "Some failure description";

        // When
        LoggedOperation loggedOperation = new LoggedOperation(
                operation,
                Optional.of(Rejected),
                Optional.of(description)
        );

        // Then
        assertThat(loggedOperation.operation, equalTo(operation));
        assertThat(loggedOperation.finalState, isPresentAndEqualTo(Rejected));
        assertThat(loggedOperation.description, isPresentAndEqualTo(description));

        assertThat(loggedOperation.isFinished(), is(true));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        Operation operation = randomOperation();
        String description = "Some failure description";

        try {
            new LoggedOperation(null, Optional.empty(), Optional.empty());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("operation can't be null"));
        }
        try {
            new LoggedOperation(operation, null, Optional.empty());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("finalState can't be null - use Optional.empty() instead"));
        }
        try {
            new LoggedOperation(operation, Optional.empty(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("description can't be null - use Optional.empty() instead"));
        }
        try {
            new LoggedOperation(operation, Optional.empty(), Optional.of(description));

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("only Rejected Operation can have description"));
        }

        try {
            new LoggedOperation(null, Optional.of(Applied), Optional.empty());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("operation can't be null"));
        }
        try {
            new LoggedOperation(operation, Optional.of(Applied), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("description can't be null - use Optional.empty() instead"));
        }
        try {
            new LoggedOperation(operation, Optional.of(Applied), Optional.of(description));

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("only Rejected Operation can have description"));
        }

        try {
            new LoggedOperation(null, Optional.of(Rejected), Optional.empty());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("operation can't be null"));
        }
        try {
            new LoggedOperation(operation, Optional.of(Rejected), null);

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("Rejected Operation must have description"));
        }
        try {
            new LoggedOperation(operation, Optional.of(Rejected), Optional.empty());

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("Rejected Operation must have description"));
        }
    }
}