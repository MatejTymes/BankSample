package mtymes.account.domain.operation;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;
import mtymes.test.StrictMockTest;
import org.junit.Test;

import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TransferDetailTest extends StrictMockTest {

    @Test
    public void shouldCreateTransferDetail() {
        AccountId fromAccountId = randomAccountId();
        AccountId toAccountId = randomAccountId();
        Decimal amount = randomPositiveAmount();

        // When
        TransferDetail transferDetail = new TransferDetail(fromAccountId, toAccountId, amount);

        // Then
        assertThat(transferDetail.fromAccountId, equalTo(fromAccountId));
        assertThat(transferDetail.toAccountId, equalTo(toAccountId));
        assertThat(transferDetail.amount, equalTo(amount));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new TransferDetail(null, randomAccountId(), randomPositiveAmount());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("fromAccountId can't be null"));
        }
        try {
            new TransferDetail(randomAccountId(), null, randomPositiveAmount());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("toAccountId can't be null"));
        }
        try {
            new TransferDetail(randomAccountId(), randomAccountId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("amount can't be null"));
        }
        try {
            new TransferDetail(randomAccountId(), randomAccountId(), Decimal.ZERO);

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
        try {
            new TransferDetail(randomAccountId(), randomAccountId(), randomNegativeAmount());

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
    }
}