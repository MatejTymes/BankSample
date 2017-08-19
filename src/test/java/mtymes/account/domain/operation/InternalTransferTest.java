package mtymes.account.domain.operation;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;
import mtymes.test.StrictMockTest;
import org.junit.Test;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javafixes.common.CollectionUtil.newSet;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InternalTransferTest extends StrictMockTest {

    @Test
    public void shouldCreateOperation() {
        AccountId fromAccountId = randomAccountId();
        AccountId toAccountId = randomAccountId();
        Decimal amount = randomPositiveDecimal();

        // When
        InternalTransfer InternalTransfer = new InternalTransfer(fromAccountId, toAccountId, amount);

        // Then
        assertThat(InternalTransfer.fromAccountId, equalTo(fromAccountId));
        assertThat(InternalTransfer.toAccountId, equalTo(toAccountId));
        assertThat(InternalTransfer.amount, equalTo(amount));
        assertThat(InternalTransfer.affectedAccountIds(), equalTo(newSet(fromAccountId, toAccountId)));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new InternalTransfer(null, randomAccountId(), randomPositiveDecimal());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("fromAccountId can't be null"));
        }
        try {
            new InternalTransfer(randomAccountId(), null, randomPositiveDecimal());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("toAccountId can't be null"));
        }
        try {
            new InternalTransfer(randomAccountId(), randomAccountId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("amount can't be null"));
        }
        try {
            new InternalTransfer(randomAccountId(), randomAccountId(), Decimal.ZERO);

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
        try {
            new InternalTransfer(randomAccountId(), randomAccountId(), randomNegativeDecimal());

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        InternalTransfer InternalTransfer = new InternalTransfer(randomAccountId(), randomAccountId(), randomPositiveDecimal());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(InternalTransfer)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = InternalTransfer.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }
}