package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.exception.DuplicateOperationException;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Condition.after;
import static mtymes.test.Condition.before;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.*;

public class TransferMoneyFromHandlerTest extends StrictMockTest {

    // todo: implement

    private AccountDao accountDao;
    private OperationDao operationDao;
    private ToProcessQueue queue;
    private TransferMoneyFromHandler handler;

    private SeqId seqId = randomSeqId();
    private TransferId transferId = randomTransferId();
    private AccountId fromAccountId = randomAccountId();
    private AccountId toAccountId = randomAccountId();
    private Decimal amount = randomPositiveDecimal();
    private TransferDetail detail = new TransferDetail(transferId, fromAccountId, toAccountId, amount);
    private TransferMoneyFrom operation = new TransferMoneyFrom(detail);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        queue = mock(ToProcessQueue.class);
        handler = new TransferMoneyFromHandler(accountDao, operationDao, queue);
    }

    @Test
    public void shouldWithdrawMoneyAndSubmitTransferToOperation() {
        SeqId lastFromAccountOpSeqId = randomSeqId(before(seqId));
        Decimal fromAccountBalance = amount.plus(randomPositiveDecimal());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .lastAppliedOpSeqId(lastFromAccountOpSeqId)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), lastFromAccountOpSeqId, seqId)).thenReturn(true);

        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.storeOperation(new TransferMoneyTo(detail))).thenReturn(randomSeqId());
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsSuccessful(seqId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperationAndTransferToOperationAlreadyExists() {
        SeqId lastFromAccountOpSeqId = randomSeqId(before(seqId));
        Decimal fromAccountBalance = amount.plus(randomPositiveDecimal());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .lastAppliedOpSeqId(lastFromAccountOpSeqId)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), lastFromAccountOpSeqId, seqId)).thenReturn(true);

        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.storeOperation(new TransferMoneyTo(detail))).thenThrow(new DuplicateOperationException());
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsSuccessful(seqId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperation() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .lastAppliedOpSeqId(seqId)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.storeOperation(new TransferMoneyTo(detail))).thenReturn(randomSeqId());
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsSuccessful(seqId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasInsufficientFunds() {
        SeqId lastFromAccountOpSeqId = randomSeqId(before(seqId));
        Decimal fromAccountBalance = randomPositiveDecimal();
        amount = fromAccountBalance.plus(randomPositiveDecimal());
        detail = new TransferDetail(transferId, fromAccountId, toAccountId, amount);
        operation = new TransferMoneyFrom(detail);
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .lastAppliedOpSeqId(lastFromAccountOpSeqId)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.markAsFailed(seqId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasZeroBalance() {
        SeqId lastFromAccountOpSeqId = randomSeqId(before(seqId));
        Decimal fromAccountBalance = Decimal.ZERO;
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .lastAppliedOpSeqId(lastFromAccountOpSeqId)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.markAsFailed(seqId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasNegativeBalance() {
        SeqId lastFromAccountOpSeqId = randomSeqId(before(seqId));
        Decimal fromAccountBalance = randomNegativeDecimal();
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .lastAppliedOpSeqId(lastFromAccountOpSeqId)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.markAsFailed(seqId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfToAccountDoesNotExist() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .build()));
        doReturn(Optional.empty()).when(accountDao).findAccount(toAccountId);
        when(operationDao.markAsFailed(seqId, "To Account '" + toAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountDoesNotExist() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.empty());
        when(operationDao.markAsFailed(seqId, "From Account '" + fromAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .lastAppliedOpSeqId(randomSeqId(after(seqId)))
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