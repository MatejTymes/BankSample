package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.InternalTransfer;
import mtymes.account.domain.operation.SeqId;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Condition.after;
import static mtymes.test.Condition.*;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.*;

public class InternalTransferHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private InternalTransferHandler handler;

    private SeqId seqId = randomOperationId();
    private AccountId fromAccountId = randomAccountId();
    private AccountId toAccountId = randomAccountId();
    private Decimal amount = randomPositiveDecimal();
    private InternalTransfer operation = new InternalTransfer(fromAccountId, toAccountId, amount);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new InternalTransferHandler(accountDao, operationDao);
    }

    @Test
    public void shouldTransferMoney() {
        SeqId lastFromAccountOpId = randomOperationId(before(seqId));
        Decimal fromAccountBalance = amount.plus(randomPositiveDecimal());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .lastAppliedOperationId(lastFromAccountOpId)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), lastFromAccountOpId, seqId)).thenReturn(true);

        SeqId lastToAccountOpId = randomOperationId(before(seqId), otherThan(lastFromAccountOpId));
        Decimal toAccountBalance = amount.plus(randomPositiveDecimal());
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .balance(toAccountBalance)
                        .lastAppliedOperationId(lastToAccountOpId)
                        .build())
        ).when(accountDao)
                .findAccount(toAccountId);
        doReturn(
                true
        ).when(accountDao)
                .updateBalance(toAccountId, toAccountBalance.plus(amount), lastToAccountOpId, seqId);

        when(operationDao.markAsSuccessful(seqId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperation() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .lastAppliedOperationId(seqId)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .lastAppliedOperationId(seqId)
                        .build())
        ).when(accountDao)
                .findAccount(toAccountId);
        when(operationDao.markAsSuccessful(seqId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasInsufficientFunds() {
        SeqId lastFromAccountOpId = randomOperationId(before(seqId));
        Decimal lastBalance = randomPositiveDecimal();
        amount = lastBalance.plus(randomPositiveDecimal());
        operation = new InternalTransfer(fromAccountId, toAccountId, amount);

        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lastFromAccountOpId)
                .build()));
        SeqId lastToAccountOpId = randomOperationId(before(seqId), otherThan(lastFromAccountOpId));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .lastAppliedOperationId(lastToAccountOpId)
                        .build())
        ).when(accountDao)
                .findAccount(toAccountId);
        when(operationDao.markAsFailed(seqId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasZeroBalance() {
        SeqId lastFromAccountOpId = randomOperationId(before(seqId));
        Decimal lastBalance = Decimal.ZERO;
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lastFromAccountOpId)
                .build()));
        SeqId lastToAccountOpId = randomOperationId(before(seqId), otherThan(lastFromAccountOpId));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .lastAppliedOperationId(lastToAccountOpId)
                        .build())
        ).when(accountDao)
                .findAccount(toAccountId);
        when(operationDao.markAsFailed(seqId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasNegativeBalance() {
        SeqId lastFromAccountOpId = randomOperationId(before(seqId));
        Decimal lastBalance = randomNegativeDecimal();
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lastFromAccountOpId)
                .build()));
        SeqId lastToAccountOpId = randomOperationId(before(seqId), otherThan(lastFromAccountOpId));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .lastAppliedOperationId(lastToAccountOpId)
                        .build())
        ).when(accountDao)
                .findAccount(toAccountId);
        when(operationDao.markAsFailed(seqId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfToAccountDoesNotExist() {
        SeqId lastFromAccountOpId = randomOperationId(before(seqId));
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .lastAppliedOperationId(lastFromAccountOpId)
                .build()));
        doReturn(Optional.empty()).when(accountDao).findAccount(toAccountId);
        when(operationDao.markAsFailed(seqId, "Account '" + toAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfFromAccountDoesNotExist() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.empty());
        when(operationDao.markAsFailed(seqId, "Account '" + fromAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .lastAppliedOperationId(randomOperationId(after(seqId)))
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .lastAppliedOperationId(randomOperationId(after(seqId)))
                        .build())
        ).when(accountDao)
                .findAccount(toAccountId);

        // When & Then
        handler.handleOperation(seqId, operation);
    }
}