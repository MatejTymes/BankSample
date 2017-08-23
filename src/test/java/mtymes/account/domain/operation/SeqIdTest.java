package mtymes.account.domain.operation;

import org.junit.Test;

import static mtymes.account.domain.operation.SeqId.seqId;
import static mtymes.test.Random.randomLong;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SeqIdTest {

    @Test
    public void shouldVerifyIfOneSeqIdIsBeforeAnother() {
        long value = randomLong(1, 9_999);

        assertThat(seqId(value).isBefore(seqId(value + 1)), is(true));
        assertThat(seqId(value).isBefore(seqId(value + randomLong(2, 9_999))), is(true));

        assertThat(seqId(value).isBefore(seqId(value - 1)), is(false));
        assertThat(seqId(value).isBefore(seqId(value - randomLong(2, 9_999))), is(false));

        assertThat(seqId(value).isBefore(seqId(value)), is(false));
    }
}