package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OpLogDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.exception.DuplicateItemException;
import mtymes.common.util.SetQueue;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Condition.after;
import static mtymes.test.Condition.before;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.*;

public class TransferFromHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private OpLogDao opLogDao;
    private SetQueue<AccountId> queue;
    private TransferFromHandler handler;

    private OperationId fromOperationId = randomOperationId();
    private OperationId toOperationId = randomOperationId();
    private AccountId fromAccountId = randomAccountId();
    private AccountId toAccountId = randomAccountId();
    private Decimal amount = randomPositiveAmount();
    private SeqId seqId = randomSeqId();
    private TransferDetail detail = new TransferDetail(fromAccountId, toAccountId, amount);
    private TransferFrom operation = new TransferFrom(fromOperationId, toOperationId, detail);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        opLogDao = mock(OpLogDao.class);
        queue = mock(SetQueue.class);
        handler = new TransferFromHandler(accountDao, operationDao, opLogDao, queue);
    }

    @Test
    public void shouldWithdrawMoneyAndSubmitTransferToOperation() {
        SeqId accountVersion = randomSeqId(before(seqId));
        Decimal fromAccountBalance = amount.plus(randomPositiveAmount());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), accountVersion, seqId)).thenReturn(true);

        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        doNothing().when(operationDao).storeOperation(new TransferTo(toOperationId, detail));
        when(opLogDao.registerOperationId(toAccountId, toOperationId)).thenReturn(randomSeqId());
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsApplied(fromOperationId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperationAndTransferToOperationAlreadyExists() {
        SeqId accountVersion = randomSeqId(before(seqId));
        Decimal fromAccountBalance = amount.plus(randomPositiveAmount());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), accountVersion, seqId)).thenReturn(true);

        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        doThrow(new DuplicateItemException()).when(operationDao).storeOperation(new TransferTo(toOperationId, detail));
        when(opLogDao.registerOperationId(toAccountId, toOperationId)).thenReturn(randomSeqId());
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsApplied(fromOperationId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperationAndTransferToOperationAlreadyExistsAndIsAlreadyRegisteredInOpLog() {
        SeqId accountVersion = randomSeqId(before(seqId));
        Decimal fromAccountBalance = amount.plus(randomPositiveAmount());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), accountVersion, seqId)).thenReturn(true);

        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        doThrow(new DuplicateItemException()).when(operationDao).storeOperation(new TransferTo(toOperationId, detail));
        doThrow(new DuplicateItemException()).when(opLogDao).registerOperationId(toAccountId, toOperationId);
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsApplied(fromOperationId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperation() {
        SeqId accountVersion = seqId;
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .version(accountVersion)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        doNothing().when(operationDao).storeOperation(new TransferTo(toOperationId, detail));
        when(opLogDao.registerOperationId(toAccountId, toOperationId)).thenReturn(randomSeqId());
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsApplied(fromOperationId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasInsufficientFunds() {
        SeqId accountVersion = randomSeqId(before(seqId));
        Decimal fromAccountBalance = randomPositiveAmount();
        amount = fromAccountBalance.plus(randomPositiveAmount());
        detail = new TransferDetail(fromAccountId, toAccountId, amount);
        operation = new TransferFrom(fromOperationId, toOperationId, detail);
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.markAsRejected(fromOperationId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasZeroBalance() {
        SeqId accountVersion = randomSeqId(before(seqId));
        Decimal fromAccountBalance = Decimal.ZERO;
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.markAsRejected(fromOperationId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasNegativeBalance() {
        SeqId accountVersion = randomSeqId(before(seqId));
        Decimal fromAccountBalance = randomNegativeAmount();
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.markAsRejected(fromOperationId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfToAccountDoesNotExist() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .build()));
        doReturn(Optional.empty()).when(accountDao).findAccount(toAccountId);
        when(operationDao.markAsRejected(fromOperationId, "To Account '" + toAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountDoesNotExist() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.empty());
        when(operationDao.markAsRejected(fromOperationId, "From Account '" + fromAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        SeqId accountVersion = randomSeqId(after(seqId));
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .version(accountVersion)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao)
                .findAccount(toAccountId);

        // When & Then
        handler.handleOperation(seqId, operation);
    }
}