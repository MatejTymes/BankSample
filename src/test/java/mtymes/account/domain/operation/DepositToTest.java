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

public class DepositToTest extends StrictMockTest {

    @Test
    public void shouldCreateOperation() {
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveDecimal();

        // When
        DepositTo depositTo = new DepositTo(accountId, amount);

        // Then
        assertThat(depositTo.accountId, equalTo(accountId));
        assertThat(depositTo.amount, equalTo(amount));
        assertThat(depositTo.affectedAccountIds(), equalTo(newSet(accountId)));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new DepositTo(null, randomPositiveDecimal());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("accountId can't be null"));
        }
        try {
            new DepositTo(randomAccountId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("amount can't be null"));
        }
        try {
            new DepositTo(randomAccountId(), Decimal.ZERO);

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
        try {
            new DepositTo(randomAccountId(), randomNegativeDecimal());

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        DepositTo depositTo = new DepositTo(randomAccountId(), randomPositiveDecimal());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(depositTo)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = depositTo.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }
}