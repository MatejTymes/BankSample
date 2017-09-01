package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.OpLogId;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.test.Condition.after;
import static mtymes.test.Condition.before;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateAccountHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private CreateAccountHandler handler;

    private AccountId accountId = randomAccountId();
    private OpLogId opLogId = randomOpLogId(accountId);
    private CreateAccount operation = new CreateAccount(accountId);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new CreateAccountHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToCreateNewAccount() {
        when(accountDao.createAccount(accountId, opLogId.version))
                .thenReturn(true);
        when(operationDao.markAsSuccessful(opLogId))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldSucceedIfAccountHasBeenAlreadyCreatedByThisOperation() {
        when(accountDao.createAccount(accountId, opLogId.version))
                .thenReturn(false);
        when(accountDao.findCurrentVersion(accountId))
                .thenReturn(Optional.of(opLogId.version));
        when(operationDao.markAsSuccessful(opLogId))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfAccountAlreadyExistedBeforeThisOperation() {
        when(accountDao.createAccount(accountId, opLogId.version))
                .thenReturn(false);
        when(accountDao.findCurrentVersion(accountId))
                .thenReturn(Optional.of(randomVersion(before(opLogId.version))));
        when(operationDao.markAsFailed(opLogId, "Account '" + accountId + "' already exists"))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfUnableToCreateAccountAndRetrieveAccountVersion() {
        when(accountDao.createAccount(accountId, opLogId.version))
                .thenReturn(false);
        when(accountDao.findCurrentVersion(accountId))
                .thenReturn(Optional.empty());
        when(operationDao.markAsFailed(opLogId, "Failed to create Account '" + accountId + "'"))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        when(accountDao.createAccount(accountId, opLogId.version))
                .thenReturn(false);
        when(accountDao.findCurrentVersion(accountId))
                .thenReturn(Optional.of(randomVersion(after(opLogId.version))));

        // When & Then
        handler.handleOperation(opLogId, operation);
    }
}