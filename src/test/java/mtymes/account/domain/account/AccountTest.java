package mtymes.account.domain.account;

import javafixes.math.Decimal;
import mtymes.account.domain.operation.SeqId;
import org.junit.Test;

import static javafixes.common.CollectionUtil.newList;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AccountTest {

    @Test
    public void shouldCreateAccount() {
        for (Decimal balance : newList(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount())) {
            AccountId accountId = randomAccountId();
            SeqId version = randomSeqId();

            // When
            Account account = new Account(accountId, balance, version);

            // Then
            assertThat(account.accountId, equalTo(accountId));
            assertThat(account.balance, equalTo(balance));
            assertThat(account.version, equalTo(version));
        }
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new Account(null, randomAmount(), randomSeqId());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("accountId can't be null"));
        }
        try {
            new Account(randomAccountId(), null, randomSeqId());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("balance can't be null"));
        }
        try {
            new Account(randomAccountId(), randomAmount(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("version can't be null"));
        }
    }
}