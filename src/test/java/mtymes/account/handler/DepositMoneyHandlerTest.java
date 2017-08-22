package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositMoney;
import mtymes.account.domain.operation.OperationId;
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

public class DepositMoneyHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private DepositMoneyHandler handler;

    private OperationId operationId = randomOperationId();
    private AccountId accountId = randomAccountId();
    private Decimal depositAmount = randomPositiveDecimal();
    private DepositMoney operation = new DepositMoney(accountId, depositAmount);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new DepositMoneyHandler(accountDao, operationDao);
    }

    @Test
    public void shouldDepositMoney() {
        OperationId lasAppliedOperationId = randomOperationId(before(operationId));
        Decimal lastBalance = randomDecimal();
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lasAppliedOperationId)
                .build()));
        when(accountDao.updateBalance(accountId, lastBalance.plus(depositAmount), lasAppliedOperationId, operationId)).thenReturn(true);
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