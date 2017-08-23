package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.SeqId;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.test.Condition.after;
import static mtymes.test.Condition.before;
import static mtymes.test.Random.randomAccountId;
import static mtymes.test.Random.randomSeqId;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateAccountHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private CreateAccountHandler handler;

    private SeqId seqId = randomSeqId();
    private AccountId accountId = randomAccountId();
    private CreateAccount operation = new CreateAccount(accountId);

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
        when(operationDao.markAsSuccessful(seqId))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfAccountHasBeenAlreadyCreatedByThisOperation() {
        when(accountDao.createAccount(accountId, seqId))
                .thenReturn(false);
        when(accountDao.findLastAppliedOpSeqId(accountId))
                .thenReturn(Optional.of(seqId));
        when(operationDao.markAsSuccessful(seqId))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfAccountAlreadyExistedBeforeThisOperation() {
        when(accountDao.createAccount(accountId, seqId))
                .thenReturn(false);
        when(accountDao.findLastAppliedOpSeqId(accountId))
                .thenReturn(Optional.of(randomSeqId(before(seqId))));
        when(operationDao.markAsFailed(seqId, "Account '" + accountId + "' already exists"))
                .thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        when(accountDao.createAccount(accountId, seqId))
                .thenReturn(false);
        when(accountDao.findLastAppliedOpSeqId(accountId))
                .thenReturn(Optional.of(randomSeqId(after(seqId))));

        // When & Then
        handler.handleOperation(seqId, operation);
    }
}