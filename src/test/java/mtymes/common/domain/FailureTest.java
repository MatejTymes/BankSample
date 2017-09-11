package mtymes.common.domain;

import org.junit.Test;

import static mtymes.common.domain.Failure.failure;
import static mtymes.test.Random.randomUUIDString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FailureTest {

    @Test
    public void shouldCreateFailure() {
        String message = "some failure message " + randomUUIDString();

        // When
        Failure failure = failure(message);

        // Then
        assertThat(failure.message, equalTo(message));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            failure(null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("message can't be null"));
        }
    }
}