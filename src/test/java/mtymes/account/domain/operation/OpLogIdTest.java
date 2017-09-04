package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
import org.junit.Test;

import static mtymes.account.domain.account.Version.version;
import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class OpLogIdTest {

    @Test
    public void shouldCreateOpLogId() {
        AccountId accountId = randomAccountId();
        Version version = randomVersion();

        // When
        OpLogId opLogId = new OpLogId(accountId, version);

        // Then
        assertThat(opLogId.accountId, equalTo(accountId));
        assertThat(opLogId.seqId, equalTo(version));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new OpLogId(null, randomVersion());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("accountId can't be null"));
        }
        try {
            new OpLogId(randomAccountId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("seqId can't be null"));
        }
    }

    @Test
    public void shouldFindIfCanApplyOperationTo() {
        AccountId accountId = randomAccountId();

        Version version = randomVersion();
        Version olderVersion = version(version.value() - randomInt(1, 10));
        Version newerVersion = version(version.value() + randomInt(1, 10));

        OpLogId opLogId = OpLogId.opLogId(accountId, version);

        // When & Then
        assertThat(opLogId.canApplyOperationTo(olderVersion), is(true));
        assertThat(opLogId.canApplyOperationTo(version), is(false));
        assertThat(opLogId.canApplyOperationTo(newerVersion), is(false));

        assertThat(opLogId.canApplyOperationTo(accountBuilder().version(olderVersion).build()), is(true));
        assertThat(opLogId.canApplyOperationTo(accountBuilder().version(version).build()), is(false));
        assertThat(opLogId.canApplyOperationTo(accountBuilder().version(newerVersion).build()), is(false));
    }

    @Test
    public void shouldFindIfIsOperationCurrentlyAppliedTo() {
        AccountId accountId = randomAccountId();

        Version version = randomVersion();
        Version olderVersion = version(version.value() - randomInt(1, 10));
        Version newerVersion = version(version.value() + randomInt(1, 10));

        OpLogId opLogId = OpLogId.opLogId(accountId, version);

        // When & Then
        assertThat(opLogId.isOperationCurrentlyAppliedTo(olderVersion), is(false));
        assertThat(opLogId.isOperationCurrentlyAppliedTo(version), is(true));
        assertThat(opLogId.isOperationCurrentlyAppliedTo(newerVersion), is(false));

        assertThat(opLogId.isOperationCurrentlyAppliedTo(accountBuilder().version(olderVersion).build()), is(false));
        assertThat(opLogId.isOperationCurrentlyAppliedTo(accountBuilder().version(version).build()), is(true));
        assertThat(opLogId.isOperationCurrentlyAppliedTo(accountBuilder().version(newerVersion).build()), is(false));
    }
}