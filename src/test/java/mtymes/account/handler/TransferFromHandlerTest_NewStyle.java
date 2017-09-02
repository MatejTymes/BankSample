package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.WorkQueue;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.exception.DuplicateOperationException;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static java.math.BigDecimal.ROUND_DOWN;
import static javafixes.math.Decimal.*;
import static mtymes.account.domain.account.Version.version;
import static mtymes.account.domain.operation.OpLogId.opLogId;
import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.*;

// todo: decide if i would like to use this style
// a) more readable
// b) some people might not be used to it = might not like it
public class TransferFromHandlerTest_NewStyle extends StrictMockTest {

    private AccountDao accountDao;
    private OperationDao operationDao;
    private WorkQueue queue;
    private TransferFromHandler handler;

    @Before
    public void setUp() throws Exception {
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        queue = mock(WorkQueue.class);
        handler = new TransferFromHandler(accountDao, operationDao, queue);
    }

    @Test
    public void shouldWithdrawMoneyAndQueueOperationTransferTo() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        Account toAccount = given_anAccountExists(randomAccount());

        OpLogId opLogId = nextOperationIdFor(fromAccount);
        Decimal amount = amountBetween(d("0.01"), fromAccount.balance);
        TransferDetail transferDetail = transferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_balanceUpdateOf(fromAccount, fromAccount.balance.minus(amount), opLogId);
        expect_storageOf(new TransferTo(transferDetail));
        expect_additionToWorkQueue(toAccount.accountId);
        expect_successOfOperation(opLogId);

        // When
        handler.handleOperation(opLogId, new TransferFrom(transferDetail));
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperationAndTransferToOperationAlreadyExists() {
        // Given
        Account fromAccount = given_anAccountExists(randomAccount());
        Account toAccount = given_anAccountExists(randomAccount());

        OpLogId opLogId = currentlyAppliedOperationIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = transferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_storageOfDuplicate(new TransferTo(transferDetail));
        expect_additionToWorkQueue(toAccount.accountId);
        expect_successOfOperation(opLogId);

        // When
        handler.handleOperation(opLogId, new TransferFrom(transferDetail));
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperation() {
        // Given
        Account fromAccount = given_anAccountExists(randomAccount());
        Account toAccount = given_anAccountExists(randomAccount());

        OpLogId opLogId = currentlyAppliedOperationIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = transferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_storageOf(new TransferTo(transferDetail));
        expect_additionToWorkQueue(toAccount.accountId);
        expect_successOfOperation(opLogId);

        // When
        handler.handleOperation(opLogId, new TransferFrom(transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountHasInsufficientFunds() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        Account toAccount = given_anAccountExists(randomAccount());

        OpLogId opLogId = nextOperationIdFor(fromAccount);
        Decimal amount = fromAccount.balance.plus(amountBetween(d("0.01"), d("1_000.00")));
        TransferDetail transferDetail = transferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_failureOfOperation(opLogId, "Insufficient funds on account '" + fromAccount.accountId + "'");

        // When
        handler.handleOperation(opLogId, new TransferFrom(transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountHasZeroBalance() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(ZERO)
                .build());
        Account toAccount = given_anAccountExists(randomAccount());

        OpLogId opLogId = nextOperationIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = transferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_failureOfOperation(opLogId, "Insufficient funds on account '" + fromAccount.accountId + "'");

        // When
        handler.handleOperation(opLogId, new TransferFrom(transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountHasNegativeBalance() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("-10_000.00"), d("-0.01")))
                .build());
        Account toAccount = given_anAccountExists(randomAccount());

        OpLogId opLogId = nextOperationIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = transferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_failureOfOperation(opLogId, "Insufficient funds on account '" + fromAccount.accountId + "'");

        // When
        handler.handleOperation(opLogId, new TransferFrom(transferDetail));
    }

    @Test
    public void shouldFailIfToAccountDoesNotExist() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        AccountId toAccountId = given_anMissingAccount();

        OpLogId opLogId = nextOperationIdFor(fromAccount);
        Decimal amount = amountBetween(d("0.01"), fromAccount.balance);
        TransferDetail transferDetail = transferDetailFor(fromAccount, toAccountId, amount);

        // Then
        expect_failureOfOperation(opLogId, "To Account '" + toAccountId + "' does not exist");

        // When
        handler.handleOperation(opLogId, new TransferFrom(transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountDoesNotExist() {
        AccountId fromAccountId = given_anMissingAccount();
        AccountId toAccountId = randomAccountId();

        OpLogId opLogId = randomOpLogId(fromAccountId);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = transferDetailFor(fromAccountId, toAccountId, amount);

        // Then
        expect_failureOfOperation(opLogId, "From Account '" + fromAccountId + "' does not exist");

        // When
        handler.handleOperation(opLogId, new TransferFrom(transferDetail));
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        Account toAccount = given_anAccountExists(randomAccount());

        OpLogId opLogId = previouslyAppliedOperationIdFor(fromAccount);
        Decimal amount = amountBetween(d("0.01"), fromAccount.balance);
        TransferDetail transferDetail = transferDetailFor(fromAccount, toAccount, amount);

        // Then
        // do nothing

        // When
        handler.handleOperation(opLogId, new TransferFrom(transferDetail));
    }


    private Account given_anAccountExists(Account account) {
        doReturn(Optional.of(account)).when(accountDao).findAccount(account.accountId);
        return account;
    }

    private AccountId given_anMissingAccount() {
        AccountId accountId = randomAccountId();
        doReturn(Optional.empty()).when(accountDao).findAccount(accountId);
        return accountId;
    }

    private OpLogId nextOperationIdFor(Account account) {
        return opLogId(
                account.accountId,
                version(account.version.value() + randomLong(1L, 10L))
        );
    }

    private OpLogId currentlyAppliedOperationIdFor(Account account) {
        return opLogId(account.accountId, account.version);
    }

    private OpLogId previouslyAppliedOperationIdFor(Account account) {
        return opLogId(
                account.accountId,
                version(account.version.value() + randomLong(-10L, -1L))
        );
    }

    private void expect_balanceUpdateOf(Account account, Decimal newBalance, OpLogId opLogId) {
        when(accountDao.updateBalance(account.accountId, newBalance, account.version, opLogId.version)).thenReturn(true);
    }

    private Account randomAccount() {
        return accountBuilder().build();
    }

    private Decimal amountBetween(Decimal fromAmount, Decimal toAmount) {
        int scaleToUse = 2;
        long fromLong = fromAmount.bigDecimalValue().setScale(scaleToUse, ROUND_DOWN).unscaledValue().longValue();
        long toLong = toAmount.bigDecimalValue().setScale(scaleToUse, ROUND_DOWN).unscaledValue().longValue();
        return decimal(randomLong(fromLong, toLong), scaleToUse);
    }

    private TransferDetail transferDetailFor(Account fromAccount, Account toAccount, Decimal amount) {
        return new TransferDetail(randomTransferId(), fromAccount.accountId, toAccount.accountId, amount);
    }

    private TransferDetail transferDetailFor(Account fromAccount, AccountId toAccountId, Decimal amount) {
        return new TransferDetail(randomTransferId(), fromAccount.accountId, toAccountId, amount);
    }

    private TransferDetail transferDetailFor(AccountId fromAccountId, AccountId toAccountId, Decimal amount) {
        return new TransferDetail(randomTransferId(), fromAccountId, toAccountId, amount);
    }

    private void expect_storageOf(Operation operation) {
        when(operationDao.storeOperation(operation)).thenReturn(randomOpLogId(operation.affectedAccountId()));
    }

    private void expect_storageOfDuplicate(Operation operation) {
        when(operationDao.storeOperation(operation)).thenThrow(new DuplicateOperationException());
    }

    private void expect_additionToWorkQueue(AccountId accountId) {
        doNothing().when(queue).add(accountId);
    }

    private void expect_successOfOperation(OpLogId opLogId) {
        when(operationDao.markAsSuccessful(opLogId)).thenReturn(true);
    }

    private void expect_failureOfOperation(OpLogId opLogId, String description) {
        when(operationDao.markAsFailed(opLogId, description)).thenReturn(true);
    }
}
