package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.exception.DuplicateItemException;
import mtymes.test.StrictMockTest;
import org.junit.Before;

import java.util.Optional;

import static mtymes.account.domain.operation.SeqId.seqId;
import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.*;

// todo: use for all operation unit tests
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

    protected SeqId generateNextSeqIdFor(Account account) {
        return seqId(account.version.value() + randomLong(1L, 10L));
    }

    protected SeqId generateCurrentlyAppliedSeqIdFor(Account account) {
        return account.version;
    }

    protected SeqId generatePreviouslyAppliedSeqIdFor(Account account) {
        return seqId(account.version.value() + randomLong(-10L, -1L));
    }

    protected Account randomAccount() {
        return accountBuilder().build();
    }

    protected Decimal amountBetween(Decimal fromAmount, Decimal toAmount) {
        return randomAmountBetween(fromAmount, toAmount);
    }

    protected TransferDetail generateTransferDetailFor(Account fromAccount, Account toAccount, Decimal amount) {
        return new TransferDetail(fromAccount.accountId, toAccount.accountId, amount);
    }

    protected TransferDetail generateTransferDetailFor(Account fromAccount, AccountId toAccountId, Decimal amount) {
        return new TransferDetail(fromAccount.accountId, toAccountId, amount);
    }

    protected TransferDetail generateTransferDetailFor(AccountId fromAccountId, Account toAccount, Decimal amount) {
        return new TransferDetail(fromAccountId, toAccount.accountId, amount);
    }

    protected TransferDetail generateTransferDetailFor(AccountId fromAccountId, AccountId toAccountId, Decimal amount) {
        return new TransferDetail(fromAccountId, toAccountId, amount);
    }

    protected void expect_balanceUpdateOf(Account account, Decimal newBalance, SeqId seqId) {
        when(accountDao.updateBalance(account.accountId, newBalance, account.version, seqId)).thenReturn(true);
    }

    protected void expect_storageOf(Operation operation) {
        doNothing().when(operationDao).storeOperation(operation);
    }

    protected void expect_storageOfDuplicate(Operation operation) {
        doThrow(new DuplicateItemException()).when(operationDao).storeOperation(operation);
    }

    protected void expect_operationMarkedAsApplied(OperationId operationId) {
        when(operationDao.markAsApplied(operationId)).thenReturn(true);
    }

    protected void expect_operationMarkedAsRejected(OperationId operationId, String description) {
        when(operationDao.markAsRejected(operationId, description)).thenReturn(true);
    }
}
