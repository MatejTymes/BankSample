package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositMoney;
import mtymes.account.domain.operation.SeqId;
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

    private SeqId seqId = randomSeqId();
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
        SeqId lastAppliedSeqId = randomSeqId(before(seqId));
        Decimal lastBalance = randomDecimal();
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .lastAppliedOpSeqId(lastAppliedSeqId)
                .build()));
        when(accountDao.updateBalance(accountId, lastBalance.plus(depositAmount), lastAppliedSeqId, seqId)).thenReturn(true);
        when(operationDao.markAsSuccessful(seqId)).thenReturn(true);

        // When & Then
        handler.handleOperation(seqId, operation);
    }

    @Test
    public void shouldSucceedIfBalanceHasBeenAlreadyUpdatedByThisOperation() {
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .lastAppliedOpSeqId(seqId)
                .build()));
        when(operationDao.markAsSuccessful(seqId)).thenReturn(true);

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
                .lastAppliedOpSeqId(randomSeqId(after(seqId)))
                .build()));

        // When & Then
        handler.handleOperation(seqId, operation);
    }
}