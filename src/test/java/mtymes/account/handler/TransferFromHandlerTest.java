package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
import mtymes.account.domain.operation.*;
import mtymes.account.exception.DuplicateOperationException;
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
    private SetQueue<AccountId> queue;
    private TransferFromHandler handler;

    private TransferId transferId = randomTransferId();
    private AccountId fromAccountId = randomAccountId();
    private AccountId toAccountId = randomAccountId();
    private Decimal amount = randomPositiveAmount();
    private OpLogId opLogId = randomOpLogId(fromAccountId);
    private TransferDetail detail = new TransferDetail(transferId, fromAccountId, toAccountId, amount);
    private TransferFrom operation = new TransferFrom(detail);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        queue = mock(SetQueue.class);
        handler = new TransferFromHandler(accountDao, operationDao, queue);
    }

    @Test
    public void shouldWithdrawMoneyAndSubmitTransferToOperation() {
        Version accountVersion = randomVersion(before(opLogId.seqId));
        Decimal fromAccountBalance = amount.plus(randomPositiveAmount());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), accountVersion, opLogId.seqId)).thenReturn(true);

        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.storeOperation(new TransferTo(detail))).thenReturn(randomOpLogId(toAccountId));
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsApplied(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperationAndTransferToOperationAlreadyExists() {
        Version accountVersion = randomVersion(before(opLogId.seqId));
        Decimal fromAccountBalance = amount.plus(randomPositiveAmount());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), accountVersion, opLogId.seqId)).thenReturn(true);

        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.storeOperation(new TransferTo(detail))).thenThrow(new DuplicateOperationException());
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsApplied(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperation() {
        Version accountVersion = opLogId.seqId;
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .version(accountVersion)
                .build()));
        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.storeOperation(new TransferTo(detail))).thenReturn(randomOpLogId(toAccountId));
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsApplied(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasInsufficientFunds() {
        Version accountVersion = randomVersion(before(opLogId.seqId));
        Decimal fromAccountBalance = randomPositiveAmount();
        amount = fromAccountBalance.plus(randomPositiveAmount());
        detail = new TransferDetail(transferId, fromAccountId, toAccountId, amount);
        operation = new TransferFrom(detail);
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

        when(operationDao.markAsRejected(opLogId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasZeroBalance() {
        Version accountVersion = randomVersion(before(opLogId.seqId));
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

        when(operationDao.markAsRejected(opLogId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasNegativeBalance() {
        Version accountVersion = randomVersion(before(opLogId.seqId));
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

        when(operationDao.markAsRejected(opLogId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfToAccountDoesNotExist() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .build()));
        doReturn(Optional.empty()).when(accountDao).findAccount(toAccountId);
        when(operationDao.markAsRejected(opLogId, "To Account '" + toAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfFromAccountDoesNotExist() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.empty());
        when(operationDao.markAsRejected(opLogId, "From Account '" + fromAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        Version accountVersion = randomVersion(after(opLogId.seqId));
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
        handler.handleOperation(opLogId, operation);
    }
}