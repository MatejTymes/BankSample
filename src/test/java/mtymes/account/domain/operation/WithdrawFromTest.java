package mtymes.account.domain.operation;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;
import mtymes.test.StrictMockTest;
import org.junit.Test;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WithdrawFromTest extends StrictMockTest {

    @Test
    public void shouldCreateOperation() {
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveDecimal();

        // When
        WithdrawFrom WithdrawFrom = new WithdrawFrom(accountId, amount);

        // Then
        assertThat(WithdrawFrom.accountId, equalTo(accountId));
        assertThat(WithdrawFrom.amount, equalTo(amount));
        assertThat(WithdrawFrom.affectedAccountId(), equalTo(accountId));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new WithdrawFrom(null, randomPositiveDecimal());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("accountId can't be null"));
        }
        try {
            new WithdrawFrom(randomAccountId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("amount can't be null"));
        }
        try {
            new WithdrawFrom(randomAccountId(), Decimal.ZERO);

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
        try {
            new WithdrawFrom(randomAccountId(), randomNegativeDecimal());

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        WithdrawFrom WithdrawFrom = new WithdrawFrom(randomAccountId(), randomPositiveDecimal());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(WithdrawFrom)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = WithdrawFrom.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }
}