package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;
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

    private SeqId seqId = randomOperationId();
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
        SeqId lastAppliedSeqId = randomOperationId(before(seqId));
        Decimal lastBalance = withdrawAmount.plus(randomPositiveDecimal());
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lastAppliedSeqId)
                .build()));
        when(accountDao.updateBalance(accountId, lastBalance.minus(withdrawAmount), lastAppliedSeqId, seqId)).thenReturn(true);
        when(operationDao.markAsSuccessful(seqId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfBalanceHasBeenAlreadyUpdatedByThisOperation() {
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .lastAppliedOperationId(seqId)
                .build()));
        when(operationDao.markAsSuccessful(seqId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfAccountHasInsufficientFunds() {
        SeqId lastAppliedSeqId = randomOperationId(before(seqId));
        Decimal lastBalance = randomPositiveDecimal();
        withdrawAmount = lastBalance.plus(randomPositiveDecimal());
        operation = new WithdrawMoney(accountId, withdrawAmount);

        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lastAppliedSeqId)
                .build()));
        when(operationDao.markAsFailed(seqId, "Insufficient funds on account '" + accountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfAccountHasZeroBalance() {
        SeqId lastAppliedSeqId = randomOperationId(before(seqId));
        Decimal lastBalance = Decimal.ZERO;
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lastAppliedSeqId)
                .build()));
        when(operationDao.markAsFailed(seqId, "Insufficient funds on account '" + accountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfAccountHasNegativeBalance() {
        SeqId lastAppliedSeqId = randomOperationId(before(seqId));
        Decimal lastBalance = randomNegativeDecimal();
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .lastAppliedOperationId(lastAppliedSeqId)
                .build()));
        when(operationDao.markAsFailed(seqId, "Insufficient funds on account '" + accountId + "'")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldFailIfAccountDoesNotExist() {
        when(accountDao.findAccount(accountId)).thenReturn(Optional.empty());
        when(operationDao.markAsFailed(seqId, "Account '" + accountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .lastAppliedOperationId(randomOperationId(after(seqId)))
                .build()));

        // When & Then
        handler.handleOperation(seqId, operation);
    }
}