package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.OperationId;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.operation.OperationId.operationId;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateAccountHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private CreateAccountHandler handler;

    private OperationId operationId = randomOperationId();
    private AccountId accountId = randomAccountId();
    private CreateAccount operation = new CreateAccount(accountId);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new CreateAccountHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToCreateAccount() {
        when(accountDao.createAccount(accountId, operationId))
                .thenReturn(true);
        when(operationDao.markAsSuccessful(operationId))
                .thenReturn(true);

        // When & Then
        handler.handleRequest(operationId, operation);
    }

    @Test
    public void shouldSucceedIfAccountHasBeenAlreadyCreatedByThisOperation() {
        when(accountDao.createAccount(accountId, operationId))
                .thenReturn(false);
        when(accountDao.findLastAppliedOperationId(accountId))
                .thenReturn(Optional.of(operationId));
        when(operationDao.markAsSuccessful(operationId))
                .thenReturn(true);

        // When & Then
        handler.handleRequest(operationId, operation);
    }

    @Test
    public void shouldFailIfAccountAlreadyExistedBeforeThisOperation() {
        when(accountDao.createAccount(accountId, operationId))
                .thenReturn(false);
        when(accountDao.findLastAppliedOperationId(accountId))
                .thenReturn(Optional.of(operationId(operationId.value() - randomInt(1, 1_000))));
        when(operationDao.markAsFailed(operationId, "Account '" + accountId + "' already exists"))
                .thenReturn(true);

        // When & Then
        handler.handleRequest(operationId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        when(accountDao.createAccount(accountId, operationId))
                .thenReturn(false);
        when(accountDao.findLastAppliedOperationId(accountId))
                .thenReturn(Optional.of(operationId(operationId.value() + randomInt(1, 1_000))));

        // When & Then
        handler.handleRequest(operationId, operation);
    }
}