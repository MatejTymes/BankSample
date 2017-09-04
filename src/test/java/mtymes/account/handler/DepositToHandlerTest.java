package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
import mtymes.account.domain.operation.DepositTo;
import mtymes.account.domain.operation.OpLogId;
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

public class DepositToHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private DepositToHandler handler;

    private AccountId accountId = randomAccountId();
    private Decimal depositAmount = randomPositiveAmount();
    private OpLogId opLogId = randomOpLogId(accountId);
    private DepositTo operation = new DepositTo(accountId, depositAmount);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new DepositToHandler(accountDao, operationDao);
    }

    @Test
    public void shouldDepositTo() {
        Version accountVersion = randomVersion(before(opLogId.seqId));
        Decimal lastBalance = randomAmount();
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .balance(lastBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(accountId, lastBalance.plus(depositAmount), accountVersion, opLogId.seqId)).thenReturn(true);
        when(operationDao.markAsApplied(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldSucceedIfBalanceHasBeenAlreadyUpdatedByThisOperation() {
        Version accountVersion = opLogId.seqId;
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .version(accountVersion)
                .build()));
        when(operationDao.markAsApplied(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfAccountDoesNotExist() {
        when(accountDao.findAccount(accountId)).thenReturn(Optional.empty());
        when(operationDao.markAsRejected(opLogId, "Account '" + accountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        Version accountVersion = randomVersion(after(opLogId.seqId));
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(accountId)
                .version(accountVersion)
                .build()));

        // When & Then
        handler.handleOperation(opLogId, operation);
    }
}