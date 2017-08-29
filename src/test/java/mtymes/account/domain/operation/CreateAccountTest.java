package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;
import mtymes.test.StrictMockTest;
import org.junit.Test;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javafixes.common.CollectionUtil.newSet;
import static mtymes.test.Random.randomAccountId;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateAccountTest extends StrictMockTest {

    @Test
    public void shouldCreateOperation() {
        AccountId accountId = randomAccountId();

        // When
        CreateAccount createAccount = new CreateAccount(accountId);

        // Then
        assertThat(createAccount.accountId, equalTo(accountId));
        assertThat(createAccount.affectedAccountId(), equalTo(newSet(accountId)));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new CreateAccount(null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("accountId can't be null"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        CreateAccount createAccount = new CreateAccount(randomAccountId());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(createAccount)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = createAccount.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }
}