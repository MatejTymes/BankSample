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

public class DepositMoneyTest extends StrictMockTest {

    @Test
    public void shouldCreateOperation() {
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveDecimal();

        // When
        DepositMoney depositMoney = new DepositMoney(accountId, amount);

        // Then
        assertThat(depositMoney.accountId, equalTo(accountId));
        assertThat(depositMoney.amount, equalTo(amount));
        assertThat(depositMoney.affectedAccountIds(), equalTo(newSet(accountId)));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new DepositMoney(null, randomPositiveDecimal());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("accountId can't be null"));
        }
        try {
            new DepositMoney(randomAccountId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("amount can't be null"));
        }
        try {
            new DepositMoney(randomAccountId(), Decimal.ZERO);

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
        try {
            new DepositMoney(randomAccountId(), randomNegativeDecimal());

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        DepositMoney depositMoney = new DepositMoney(randomAccountId(), randomPositiveDecimal());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(depositMoney)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = depositMoney.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }
}