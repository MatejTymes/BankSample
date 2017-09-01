package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferId;
import mtymes.account.domain.operation.TransferTo;
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

public class TransferToHandlerTest extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private TransferToHandler handler;

    private TransferId transferId = randomTransferId();
    private AccountId fromAccountId = randomAccountId();
    private AccountId toAccountId = randomAccountId();
    private Decimal amount = randomPositiveDecimal();
    private OpLogId opLogId = randomOpLogId(toAccountId);
    private TransferDetail detail = new TransferDetail(transferId, fromAccountId, toAccountId, amount);
    private TransferTo operation = new TransferTo(detail);

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        handler = new TransferToHandler(accountDao, operationDao);
    }

    @Test
    public void shouldDepositTo() {
        Version accountVersion = randomVersion(before(opLogId.version));
        Decimal lastBalance = randomDecimal();
        when(accountDao.findAccount(toAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(toAccountId)
                .balance(lastBalance)
                .version(accountVersion)
                .build()));
        when(accountDao.updateBalance(toAccountId, lastBalance.plus(amount), accountVersion, opLogId.version)).thenReturn(true);
        when(operationDao.markAsSuccessful(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldSucceedIfBalanceHasBeenAlreadyUpdatedByThisOperation() {
        Version accountVersion = opLogId.version;
        when(accountDao.findAccount(toAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(toAccountId)
                .version(accountVersion)
                .build()));
        when(operationDao.markAsSuccessful(opLogId)).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldFailIfAccountDoesNotExist() {
        when(accountDao.findAccount(toAccountId)).thenReturn(Optional.empty());
        when(operationDao.markAsFailed(opLogId, "To Account '" + toAccountId + "' does not exist")).thenReturn(true);

        // When & Then
        handler.handleOperation(opLogId, operation);
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        Version accountVersion = randomVersion(after(opLogId.version));
        when(accountDao.findAccount(toAccountId)).thenReturn(Optional.of(accountBuilder()
                .accountId(toAccountId)
                .version(accountVersion)
                .build()));

        // When & Then
        handler.handleOperation(opLogId, operation);
    }
}