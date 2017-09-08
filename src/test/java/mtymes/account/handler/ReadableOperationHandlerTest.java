package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.exception.DuplicateOperationException;
import mtymes.test.StrictMockTest;
import org.junit.Before;

import java.util.Optional;

import static java.math.BigDecimal.ROUND_DOWN;
import static javafixes.math.Decimal.decimal;
import static mtymes.account.domain.account.Version.version;
import static mtymes.account.domain.operation.OpLogId.opLogId;
import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.*;

public abstract class ReadableOperationHandlerTest extends StrictMockTest {

    protected AccountDao accountDao;
    protected OperationDao operationDao;

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
    }

    protected Account given_anAccountExists(Account account) {
        doReturn(Optional.of(account)).when(accountDao).findAccount(account.accountId);
        return account;
    }

    protected Account given_anAccountExists() {
        return given_anAccountExists(randomAccount());
    }

    protected AccountId given_anMissingAccount() {
        AccountId accountId = randomAccountId();
        doReturn(Optional.empty()).when(accountDao).findAccount(accountId);
        return accountId;
    }

    protected OpLogId generateNextOperationIdFor(Account account) {
        return opLogId(
                account.accountId,
                version(account.version.value() + randomLong(1L, 10L))
        );
    }

    protected OpLogId generateCurrentlyAppliedOperationIdFor(Account account) {
        return opLogId(account.accountId, account.version);
    }

    protected OpLogId generatePreviouslyAppliedOperationIdFor(Account account) {
        return opLogId(
                account.accountId,
                version(account.version.value() + randomLong(-10L, -1L))
        );
    }

    protected Account randomAccount() {
        return accountBuilder().build();
    }

    protected Decimal amountBetween(Decimal fromAmount, Decimal toAmount) {
        int scaleToUse = 2;
        long fromLong = fromAmount.bigDecimalValue().setScale(scaleToUse, ROUND_DOWN).unscaledValue().longValue();
        long toLong = toAmount.bigDecimalValue().setScale(scaleToUse, ROUND_DOWN).unscaledValue().longValue();
        return decimal(randomLong(fromLong, toLong), scaleToUse);
    }

    protected TransferDetail generateTransferDetailFor(Account fromAccount, Account toAccount, Decimal amount) {
        return new TransferDetail(randomTransferId(), fromAccount.accountId, toAccount.accountId, amount);
    }

    protected TransferDetail generateTransferDetailFor(Account fromAccount, AccountId toAccountId, Decimal amount) {
        return new TransferDetail(randomTransferId(), fromAccount.accountId, toAccountId, amount);
    }

    protected TransferDetail generateTransferDetailFor(AccountId fromAccountId, Account toAccount, Decimal amount) {
        return new TransferDetail(randomTransferId(), fromAccountId, toAccount.accountId, amount);
    }

    protected TransferDetail generateTransferDetailFor(AccountId fromAccountId, AccountId toAccountId, Decimal amount) {
        return new TransferDetail(randomTransferId(), fromAccountId, toAccountId, amount);
    }

    protected void expect_balanceUpdateOf(Account account, Decimal newBalance, OpLogId opLogId) {
        when(accountDao.updateBalance(account.accountId, newBalance, account.version, opLogId.seqId)).thenReturn(true);
    }

    protected void expect_storageOf(Operation operation) {
        when(operationDao.storeOperation(operation)).thenReturn(randomOpLogId(operation.affectedAccountId()));
    }

    protected void expect_storageOfDuplicate(Operation operation) {
        when(operationDao.storeOperation(operation)).thenThrow(new DuplicateOperationException());
    }

    protected void expect_operationMarkedAsApplied(OpLogId opLogId) {
        when(operationDao.markAsApplied(opLogId)).thenReturn(true);
    }

    protected void expect_operationMarkedAsRejected(OpLogId opLogId, String description) {
        when(operationDao.markAsRejected(opLogId, description)).thenReturn(true);
    }
}
