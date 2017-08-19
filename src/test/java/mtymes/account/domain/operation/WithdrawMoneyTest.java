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

public class WithdrawMoneyTest  extends StrictMockTest {

    @Test
    public void shouldCreateOperation() {
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveDecimal();

        // When
        WithdrawMoney WithdrawMoney = new WithdrawMoney(accountId, amount);

        // Then
        assertThat(WithdrawMoney.accountId, equalTo(accountId));
        assertThat(WithdrawMoney.amount, equalTo(amount));
        assertThat(WithdrawMoney.affectedAccountIds(), equalTo(newSet(accountId)));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new WithdrawMoney(null, randomPositiveDecimal());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("accountId can't be null"));
        }
        try {
            new WithdrawMoney(randomAccountId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("amount can't be null"));
        }
        try {
            new WithdrawMoney(randomAccountId(), Decimal.ZERO);

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
        try {
            new WithdrawMoney(randomAccountId(), randomNegativeDecimal());

            fail("should fail with IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("amount must be a positive value"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        WithdrawMoney WithdrawMoney = new WithdrawMoney(randomAccountId(), randomPositiveDecimal());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(WithdrawMoney)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = WithdrawMoney.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }
}