package mtymes.account.domain.operation;

import org.hamcrest.Matchers;
import org.junit.Test;

import static mtymes.account.domain.operation.SeqId.seqId;
import static mtymes.test.Random.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SeqIdTest {

    @Test
    public void shouldCompareTwoSeqIds() {
        long value = randomLong(1, 9_999);
        long biggerValue = value + randomLong(1, 9_9999);
        long smallerValue = value - randomLong(1, 9_999);

        assertThat(seqId(value).compareTo(seqId(value)), is(0));
        assertThat(seqId(value).compareTo(seqId(biggerValue)), is(-1));
        assertThat(seqId(value).compareTo(seqId(smallerValue)), is(1));
    }

    @Test
    public void shouldVerifyIfOneSeqIdIsBeforeAnother() {
        long value = randomLong(1, 9_999);

        assertThat(seqId(value).isBefore(seqId(value + 1)), is(true));
        assertThat(seqId(value).isBefore(seqId(value + randomLong(2, 9_999))), is(true));

        assertThat(seqId(value).isBefore(seqId(value - 1)), is(false));
        assertThat(seqId(value).isBefore(seqId(value - randomLong(2, 9_999))), is(false));

        assertThat(seqId(value).isBefore(seqId(value)), is(false));
    }

    @Test
    public void shouldFindIfCanApplySeqIdTo() {
        SeqId seqId = randomSeqId();
        SeqId olderSeqId = seqId(seqId.value() - randomInt(1, 10));
        SeqId newerSeqId = seqId(seqId.value() + randomInt(1, 10));

        // When & Then
        assertThat(seqId.canApplyAfter(olderSeqId), Matchers.is(true));
        assertThat(seqId.canApplyAfter(seqId), Matchers.is(false));
        assertThat(seqId.canApplyAfter(newerSeqId), Matchers.is(false));
    }

    @Test
    public void shouldFindIfSeqIdIsCurrentlyAppliedTo() {
        SeqId seqId = randomSeqId();
        SeqId olderSeqId = seqId(seqId.value() - randomInt(1, 10));
        SeqId newerSeqId = seqId(seqId.value() + randomInt(1, 10));

        // When & Then
        assertThat(seqId.isCurrentlyApplied(olderSeqId), Matchers.is(false));
        assertThat(seqId.isCurrentlyApplied(seqId), Matchers.is(true));
        assertThat(seqId.isCurrentlyApplied(newerSeqId), Matchers.is(false));
    }
}