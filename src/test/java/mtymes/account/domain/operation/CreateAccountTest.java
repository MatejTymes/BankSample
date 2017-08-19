package mtymes.account.domain.operation;

import mtymes.account.domain.account.AccountId;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javafixes.common.CollectionUtil.newSet;
import static mtymes.test.Random.randomAccountId;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CreateAccountTest {

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Test(expected = NullPointerException.class)
    public void shouldFailConstructionIfAccountIdIsNull() {
        new CreateAccount(null);
    }

    @Test
    public void shouldProvideAffectedAccountIds() {
        AccountId accountId = randomAccountId();
        CreateAccount createAccount = new CreateAccount(accountId);

        assertThat(createAccount.affectedAccountIds(), equalTo(newSet(accountId)));
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
        verifyNoMoreInteractions(visitor);
    }
}