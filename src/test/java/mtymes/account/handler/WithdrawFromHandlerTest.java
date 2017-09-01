package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.WithdrawFrom;
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

public class WithdrawFromHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private WithdrawFromHandler handler;

    private AccountId accountId = randomAccountId();
    private Decimal withdrawAmount = randomPositiveAmount();
    private OpLogId opLogId = randomOpLogId(accountId);
    private WithdrawFrom operation = new WithdrawFrom(accountId, withdrawAmount);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new WithdrawFromHandler(accountDao, operationDao);
    }

    @Test
    public void shouldWithdrawFrom() {
        Version accountVersion = randomVersion(before(opLogId.version));
        Decimal lastBalance = withdrawAmount.plus(randomPositiveAmount());
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(accountId, lastBalance.minus(withdrawAmount), accountVersion, opLogId.version)).thenReturn(true);
        when(operationDao.markAsSuccessful(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldSucceedIfBalanceHasBeenAlreadyUpdatedByThisOperation() {
        Version accountVersion = opLogId.version;
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .version(accountVersion)
                .build()));
        when(operationDao.markAsSuccessful(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfAccountHasInsufficientFunds() {
        Version accountVersion = randomVersion(before(opLogId.version));
        Decimal lastBalance = randomPositiveAmount();
        withdrawAmount = lastBalance.plus(randomPositiveAmount());
        operation = new WithdrawFrom(accountId, withdrawAmount);

        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .version(accountVersion)
                .build()));
        when(operationDao.markAsFailed(opLogId, "Insufficient funds on account '" + accountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfAccountHasZeroBalance() {
        Version accountVersion = randomVersion(before(opLogId.version));
        Decimal lastBalance = Decimal.ZERO;
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .version(accountVersion)
                .build()));
        when(operationDao.markAsFailed(opLogId, "Insufficient funds on account '" + accountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfAccountHasNegativeBalance() {
        Version accountVersion = randomVersion(before(opLogId.version));
        Decimal lastBalance = randomNegativeAmount();
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .version(accountVersion)
                .build()));
        when(operationDao.markAsFailed(opLogId, "Insufficient funds on account '" + accountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfAccountDoesNotExist() {
        when(accountDao.findAccount(accountId)).thenReturn(Optional.empty());
        when(operationDao.markAsFailed(opLogId, "Account '" + accountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        Version accountVersion = randomVersion(after(opLogId.version));
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .version(accountVersion)
                .build()));

        // When & Then
        handler.handleOperation(opLogId, operation);
    }
}