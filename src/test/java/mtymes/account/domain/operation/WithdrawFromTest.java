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
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveAmount();

        // When
        WithdrawFrom WithdrawFrom = new WithdrawFrom(operationId, accountId, amount);

        // Then
        assertThat(WithdrawFrom.operationId, equalTo(operationId));
        assertThat(WithdrawFrom.accountId, equalTo(accountId));
        assertThat(WithdrawFrom.amount, equalTo(amount));
        assertThat(WithdrawFrom.affectedAccountId(), equalTo(accountId));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new WithdrawFrom(null, randomAccountId(), randomPositiveAmount());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("operationId can't be null"));
        }
        try {
            new WithdrawFrom(randomOperationId(), null, randomPositiveAmount());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("accountId can't be null"));
        }
        try {
            new WithdrawFrom(randomOperationId(), randomAccountId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("amount can't be null"));
        }
        try {
            new WithdrawFrom(randomOperationId(), randomAccountId(), Decimal.ZERO);

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
        try {
            new WithdrawFrom(randomOperationId(), randomAccountId(), randomNegativeAmount());

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        WithdrawFrom WithdrawFrom = new WithdrawFrom(randomOperationId(), randomAccountId(), randomPositiveAmount());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(WithdrawFrom)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = WithdrawFrom.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }
}