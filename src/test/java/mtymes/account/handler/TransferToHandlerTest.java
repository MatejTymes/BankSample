package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferTo;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Condition.after;
import static mtymes.test.Condition.before;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransferToHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private TransferToHandler handler;

    private OperationId operationId = randomOperationId();
    private AccountId fromAccountId = randomAccountId();
    private AccountId toAccountId = randomAccountId();
    private Decimal amount = randomPositiveAmount();
    private SeqId seqId = randomSeqId();
    private TransferDetail detail = new TransferDetail(fromAccountId, toAccountId, amount);
    private TransferTo operation = new TransferTo(operationId, detail);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new TransferToHandler(accountDao, operationDao);
    }

    @Test
    public void shouldDepositTo() {
        SeqId accountVersion = randomSeqId(before(seqId));
        Decimal lastBalance = randomAmount();
        when(accountDao.findAccount(toAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(toAccountId)
                .balance(lastBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(toAccountId, lastBalance.plus(amount), accountVersion, seqId)).thenReturn(true);
        when(operationDao.markAsApplied(operationId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfBalanceHasBeenAlreadyUpdatedByThisOperation() {
        SeqId accountVersion = seqId;
        when(accountDao.findAccount(toAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(toAccountId)
                .version(accountVersion)
                .build()));
        when(operationDao.markAsApplied(operationId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfAccountDoesNotExist() {
        when(accountDao.findAccount(toAccountId)).thenReturn(Optional.empty());
        when(operationDao.markAsRejected(operationId, "To Account '" + toAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        SeqId accountVersion = randomSeqId(after(seqId));
        when(accountDao.findAccount(toAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(toAccountId)
                .version(accountVersion)
                .build()));

        // When & Then
        handler.handleOperation(seqId, operation);
    }
}