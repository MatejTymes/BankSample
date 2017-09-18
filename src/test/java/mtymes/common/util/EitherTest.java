package mtymes.common.util;

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class EitherTest {

    @Test
    public void shouldHandleRight() {
        UUID value = randomUUID();
        Either<Integer, UUID> either = Either.right(value);

        assertThat(either.isRight(), is(true));
        assertThat(either.isLeft(), is(false));
        assertThat(either.getRight(), equalTo(value));
        try {
            either.getLeft();

            fail("expected NoSuchElementException");
        } catch (NoSuchElementException expectedException) {
            assertThat(expectedException.getMessage(), equalTo("Left value not defined"));
        }
        AtomicBoolean handledLeft = new AtomicBoolean(false);
        AtomicBoolean handledRight = new AtomicBoolean(false);
        Object receivedValue = either.handleAndGet(
                () -> handledRight.set(true),
                () -> handledLeft.set(true)
        );
        assertThat(receivedValue, equalTo(value));
        assertThat(handledRight.get(), is(true));
        assertThat(handledLeft.get(), is(false));
        assertThat(either.get(), equalTo(value));
    }

    @Test
    public void shouldHandleLeft() {
        UUID value = randomUUID();
        Either<UUID, Integer> either = Either.left(value);

        assertThat(either.isRight(), is(false));
        assertThat(either.isLeft(), is(true));
        try {
            either.getRight();

            fail("expected NoSuchElementException");
        } catch (NoSuchElementException expectedException) {
            assertThat(expectedException.getMessage(), equalTo("Right value not defined"));
        }
        assertThat(either.getLeft(), equalTo(value));
        AtomicBoolean handledLeft = new AtomicBoolean(false);
        AtomicBoolean handledRight = new AtomicBoolean(false);
        Object receivedValue = either.handleAndGet(
                () -> handledRight.set(true),
                () -> handledLeft.set(true)
        );
        assertThat(receivedValue, equalTo(value));
        assertThat(handledRight.get(), is(false));
        assertThat(handledLeft.get(), is(true));
        assertThat(either.get(), equalTo(value));
    }
}