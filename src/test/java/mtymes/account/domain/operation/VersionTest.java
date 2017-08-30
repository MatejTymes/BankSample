package mtymes.account.domain.operation;

import org.junit.Test;

import static mtymes.account.domain.account.Version.version;
import static mtymes.test.Random.randomLong;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class VersionTest {

    @Test
    public void shouldCompareTwoVersions() {
        long value = randomLong(1, 9_999);
        long biggerValue = value + randomLong(1, 9_9999);
        long smallerValue = value - randomLong(1, 9_999);

        assertThat(version(value).compareTo(version(value)), is(0));
        assertThat(version(value).compareTo(version(biggerValue)), is(-1));
        assertThat(version(value).compareTo(version(smallerValue)), is(1));
    }

    @Test
    public void shouldVerifyIfOneVersionIsBeforeAnother() {
        long value = randomLong(1, 9_999);

        assertThat(version(value).isBefore(version(value + 1)), is(true));
        assertThat(version(value).isBefore(version(value + randomLong(2, 9_999))), is(true));

        assertThat(version(value).isBefore(version(value - 1)), is(false));
        assertThat(version(value).isBefore(version(value - randomLong(2, 9_999))), is(false));

        assertThat(version(value).isBefore(version(value)), is(false));
    }

}