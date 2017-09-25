package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;
import mtymes.test.StrictMockTest;
import org.junit.Test;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static mtymes.test.Random.randomAccountId;
import static mtymes.test.Random.randomOperationId;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateAccountTest extends StrictMockTest {

    @Test
    public void shouldCreateOperation() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();

        // When
        CreateAccount createAccount = new CreateAccount(operationId, accountId);

        // Then
        assertThat(createAccount.operationId, equalTo(operationId));
        assertThat(createAccount.accountId, equalTo(accountId));
        assertThat(createAccount.affectedAccountId(), equalTo(accountId));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new CreateAccount(null, randomAccountId());

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("operationId can't be null"));
        }
        try {
            new CreateAccount(randomOperationId(), null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("accountId can't be null"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        CreateAccount createAccount = new CreateAccount(randomOperationId(), randomAccountId());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(createAccount)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = createAccount.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }
}