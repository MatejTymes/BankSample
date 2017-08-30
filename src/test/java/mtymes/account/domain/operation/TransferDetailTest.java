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
        TransferId transferId = randomTransferId();
        AccountId fromAccountId = randomAccountId();
        AccountId toAccountId = randomAccountId();
        Decimal amount = randomPositiveDecimal();

        // When
        TransferDetail transferDetail = new TransferDetail(transferId, fromAccountId, toAccountId, amount);

        // Then
        assertThat(transferDetail.transferId, equalTo(transferId));
        assertThat(transferDetail.fromAccountId, equalTo(fromAccountId));
        assertThat(transferDetail.toAccountId, equalTo(toAccountId));
        assertThat(transferDetail.amount, equalTo(amount));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new TransferDetail(null, randomAccountId(), randomAccountId(), randomPositiveDecimal());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("transferId can't be null"));
        }
        try {
            new TransferDetail(randomTransferId(), null, randomAccountId(), randomPositiveDecimal());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("fromAccountId can't be null"));
        }
        try {
            new TransferDetail(randomTransferId(), randomAccountId(), null, randomPositiveDecimal());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("toAccountId can't be null"));
        }
        try {
            new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("amount can't be null"));
        }
        try {
            new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), Decimal.ZERO);

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
        try {
            new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomNegativeDecimal());

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
    }
}