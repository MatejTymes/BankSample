package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
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

    private OperationId operationId = randomOperationId();
    private AccountId accountId = randomAccountId();
    private SeqId seqId = randomSeqId();
    private CreateAccount operation = new CreateAccount(operationId, accountId);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new CreateAccountHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToCreateNewAccount() {
        when(accountDao.createAccount(accountId, seqId))
                .thenReturn(true);
        when(operationDao.markAsApplied(operationId))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfAccountHasBeenAlreadyCreatedByThisOperation() {
        when(accountDao.createAccount(accountId, seqId))
                .thenReturn(false);
        when(accountDao.findCurrentVersion(accountId))
                .thenReturn(Optional.of(seqId));
        when(operationDao.markAsApplied(operationId))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfAccountAlreadyExistedBeforeThisOperation() {
        when(accountDao.createAccount(accountId, seqId))
                .thenReturn(false);
        when(accountDao.findCurrentVersion(accountId))
                .thenReturn(Optional.of(randomSeqId(before(seqId))));
        when(operationDao.markAsRejected(operationId, "Account '" + accountId + "' already exists"))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfUnableToCreateAccountAndRetrieveAccountVersion() {
        when(accountDao.createAccount(accountId, seqId))
                .thenReturn(false);
        when(accountDao.findCurrentVersion(accountId))
                .thenReturn(Optional.empty());
        when(operationDao.markAsRejected(operationId, "Failed to create Account '" + accountId + "'"))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        when(accountDao.createAccount(accountId, seqId))
                .thenReturn(false);
        when(accountDao.findCurrentVersion(accountId))
                .thenReturn(Optional.of(randomSeqId(after(seqId))));

        // When & Then
        handler.handleOperation(seqId, operation);
    }
}