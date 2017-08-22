package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.WithdrawMoney;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Condition.after;
import static mtymes.test.Condition.before;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WithdrawMoneyHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private WithdrawMoneyHandler handler;

    private OperationId operationId = randomOperationId();
    private AccountId accountId = randomAccountId();
    private Decimal withdrawAmount = randomPositiveDecimal();
    private WithdrawMoney operation = new WithdrawMoney(accountId, withdrawAmount);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new WithdrawMoneyHandler(accountDao, operationDao);
    }

    @Test
    public void shouldWithdrawMoney() {
        OperationId lasAppliedOperationId = randomOperationId(before(operationId));
        Decimal lastBalance = withdrawAmount.plus(randomPositiveDecimal());
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lasAppliedOperationId)
                .build()));
        when(accountDao.updateBalance(accountId, lastBalance.minus(withdrawAmount), lasAppliedOperationId, operationId)).thenReturn(true);
        when(operationDao.markAsSuccessful(operationId)).thenReturn(true);

        // When & Then
        handler.handleOperation(operationId, operation);
    }

    @Test
    public void shouldSucceedIfBalanceHasBeenAlreadyUpdatedByThisOperation() {
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .lastAppliedOperationId(operationId)
                .build()));
        when(operationDao.markAsSuccessful(operationId)).thenReturn(true);

        // When & Then
        handler.handleOperation(operationId, operation);
    }

//    @Test
//    public void shouldFailIfAccountHasInsufficientFunds() {
//        OperationId lasAppliedOperationId = randomOperationId(before(operationId));
//        Decimal lastBalance = randomDecimal("0.01", withdrawAmount.minus(d("0.01")).toPlainString());
//        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
//                .accountId(accountId)
//                .balance(lastBalance)
//                .lastAppliedOperationId(lasAppliedOperationId)
//                .build()));
//        when(operationDao.markAsFailed(operationId, "Insufficient funds on account '" + accountId + "'")).thenReturn(true);
//
//        // When & Then
//        handler.handleOperation(operationId, operation);
//    }

    @Test
    public void shouldFailIfAccountHasZeroBalance() {
        OperationId lasAppliedOperationId = randomOperationId(before(operationId));
        Decimal lastBalance = Decimal.ZERO;
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lasAppliedOperationId)
                .build()));
        when(operationDao.markAsFailed(operationId, "Insufficient funds on account '" + accountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(operationId, operation);
    }

    @Test
    public void shouldFailIfAccountHasNegativeBalance() {
        OperationId lasAppliedOperationId = randomOperationId(before(operationId));
        Decimal lastBalance = randomNegativeDecimal();
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lasAppliedOperationId)
                .build()));
        when(operationDao.markAsFailed(operationId, "Insufficient funds on account '" + accountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(operationId, operation);
    }

    @Test
    public void shouldFailIfAccountDoesNotExist() {
        when(accountDao.findAccount(accountId)).thenReturn(Optional.empty());
        when(operationDao.markAsFailed(operationId, "Account '" + accountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(operationId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .lastAppliedOperationId(randomOperationId(after(operationId)))
                .build()));

        // When & Then
        handler.handleOperation(operationId, operation);
    }
}