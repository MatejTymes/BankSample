package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
import org.junit.Test;

import static mtymes.test.Random.randomAccountId;
import static mtymes.test.Random.randomVersion;
import static org.hamcrest.Matchers.equalTo;
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
}