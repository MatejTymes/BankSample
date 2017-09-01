package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.ToProcessQueue;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
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

public class TransferFromHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private ToProcessQueue queue;
    private TransferFromHandler handler;

    private TransferId transferId = randomTransferId();
    private AccountId fromAccountId = randomAccountId();
    private AccountId toAccountId = randomAccountId();
    private Decimal amount = randomPositiveDecimal();
    private OpLogId opLogId = randomOpLogId(fromAccountId);
    private TransferDetail detail = new TransferDetail(transferId, fromAccountId, toAccountId, amount);
    private TransferFrom operation = new TransferFrom(detail);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        queue = mock(ToProcessQueue.class);
        handler = new TransferFromHandler(accountDao, operationDao, queue);
    }

    @Test
    public void shouldWithdrawMoneyAndSubmitTransferToOperation() {
        Version accountVersion = randomVersion(before(opLogId.version));
        Decimal fromAccountBalance = amount.plus(randomPositiveDecimal());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), accountVersion, opLogId.version)).thenReturn(true);

        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.storeOperation(new TransferTo(detail))).thenReturn(randomOpLogId(toAccountId));
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsSuccessful(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperationAndTransferToOperationAlreadyExists() {
        Version accountVersion = randomVersion(before(opLogId.version));
        Decimal fromAccountBalance = amount.plus(randomPositiveDecimal());
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .balance(fromAccountBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(fromAccountId, fromAccountBalance.minus(amount), accountVersion, opLogId.version)).thenReturn(true);

        doReturn(
                Optional.of(accountBuilder()
                        .accountId(toAccountId)
                        .build())
        ).when(accountDao).findAccount(toAccountId);

        when(operationDao.storeOperation(new TransferTo(detail))).thenThrow(new DuplicateOperationException());
        doNothing().when(queue).add(toAccountId);

        when(operationDao.markAsSuccessful(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperation() {
        Version accountVersion = opLogId.version;
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

        when(operationDao.markAsSuccessful(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasInsufficientFunds() {
        Version accountVersion = randomVersion(before(opLogId.version));
        Decimal fromAccountBalance = randomPositiveDecimal();
        amount = fromAccountBalance.plus(randomPositiveDecimal());
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

        when(operationDao.markAsFailed(opLogId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasZeroBalance() {
        Version accountVersion = randomVersion(before(opLogId.version));
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

        when(operationDao.markAsFailed(opLogId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfFromAccountHasNegativeBalance() {
        Version accountVersion = randomVersion(before(opLogId.version));
        Decimal fromAccountBalance = randomNegativeDecimal();
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

        when(operationDao.markAsFailed(opLogId, "Insufficient funds on account '" + fromAccountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfToAccountDoesNotExist() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(fromAccountId)
                .build()));
        doReturn(Optional.empty()).when(accountDao).findAccount(toAccountId);
        when(operationDao.markAsFailed(opLogId, "To Account '" + toAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfFromAccountDoesNotExist() {
        when(accountDao.findAccount(fromAccountId)).thenReturn(Optional.empty());
        when(operationDao.markAsFailed(opLogId, "From Account '" + fromAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        Version accountVersion = randomVersion(after(opLogId.version));
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