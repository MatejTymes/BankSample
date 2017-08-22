package mtymes.account.domain.operation;

import org.junit.Test;

import static mtymes.account.domain.operation.OperationId.operationId;
import static mtymes.test.Random.randomLong;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class OperationIdTest {

    @Test
    public void shouldVerifyIfOneOperationIdIsBeforeAnother() {
        long value = randomLong(1, 9_999);

        assertThat(operationId(value).isBefore(operationId(value + 1)), is(true));
        assertThat(operationId(value).isBefore(operationId(value + randomLong(2, 9_999))), is(true));

        assertThat(operationId(value).isBefore(operationId(value - 1)), is(false));
        assertThat(operationId(value).isBefore(operationId(value - randomLong(2, 9_999))), is(false));

        assertThat(operationId(value).isBefore(operationId(value)), is(false));
    }
}