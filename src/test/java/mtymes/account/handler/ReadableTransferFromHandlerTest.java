package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.common.util.SetQueue;
import org.junit.Before;
import org.junit.Test;

import static javafixes.math.Decimal.ZERO;
import static javafixes.math.Decimal.d;
import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

public class ReadableTransferFromHandlerTest extends ReadableOperationHandlerTest {

    private SetQueue<AccountId> queue;
    private TransferFromHandler handler;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        queue = mock(SetQueue.class);
        handler = new TransferFromHandler(accountDao, operationDao, queue);
    }

    @Test
    public void shouldWithdrawMoneyAndQueueOperationTransferTo() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        Account toAccount = given_anAccountExists();

        OperationId toPartOperationId = randomOperationId();
        OpLogId opLogId = generateNextOperationIdFor(fromAccount);
        Decimal amount = amountBetween(d("0.01"), fromAccount.balance);
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_balanceUpdateOf(fromAccount, fromAccount.balance.minus(amount), opLogId);
        expect_storageOf(new TransferTo(toPartOperationId, transferDetail));
        expect_additionToWorkQueue(toAccount.accountId);
        expect_operationMarkedAsApplied(opLogId);

        // When
        handler.handleOperation(opLogId, new TransferFrom(randomOperationId(), toPartOperationId, transferDetail));
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperationAndTransferToOperationAlreadyExists() {
        // Given
        Account fromAccount = given_anAccountExists();
        Account toAccount = given_anAccountExists();

        OperationId toPartOperationId = randomOperationId();
        OpLogId opLogId = generateCurrentlyAppliedOperationIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_storageOfDuplicate(new TransferTo(toPartOperationId, transferDetail));
        expect_additionToWorkQueue(toAccount.accountId);
        expect_operationMarkedAsApplied(opLogId);

        // When
        handler.handleOperation(opLogId, new TransferFrom(randomOperationId(), toPartOperationId, transferDetail));
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperation() {
        // Given
        Account fromAccount = given_anAccountExists();
        Account toAccount = given_anAccountExists();

        OperationId toPartOperationId = randomOperationId();
        OpLogId opLogId = generateCurrentlyAppliedOperationIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_storageOf(new TransferTo(toPartOperationId, transferDetail));
        expect_additionToWorkQueue(toAccount.accountId);
        expect_operationMarkedAsApplied(opLogId);

        // When
        handler.handleOperation(opLogId, new TransferFrom(randomOperationId(), toPartOperationId, transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountHasInsufficientFunds() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        Account toAccount = given_anAccountExists();

        OpLogId opLogId = generateNextOperationIdFor(fromAccount);
        Decimal amount = fromAccount.balance.plus(amountBetween(d("0.01"), d("1_000.00")));
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_operationMarkedAsRejected(opLogId, "Insufficient funds on account '" + fromAccount.accountId + "'");

        // When
        handler.handleOperation(opLogId, new TransferFrom(randomOperationId(), randomOperationId(), transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountHasZeroBalance() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(ZERO)
                .build());
        Account toAccount = given_anAccountExists();

        OpLogId opLogId = generateNextOperationIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_operationMarkedAsRejected(opLogId, "Insufficient funds on account '" + fromAccount.accountId + "'");

        // When
        handler.handleOperation(opLogId, new TransferFrom(randomOperationId(), randomOperationId(), transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountHasNegativeBalance() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("-10_000.00"), d("-0.01")))
                .build());
        Account toAccount = given_anAccountExists();

        OpLogId opLogId = generateNextOperationIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_operationMarkedAsRejected(opLogId, "Insufficient funds on account '" + fromAccount.accountId + "'");

        // When
        handler.handleOperation(opLogId, new TransferFrom(randomOperationId(), randomOperationId(), transferDetail));
    }

    @Test
    public void shouldFailIfToAccountDoesNotExist() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        AccountId toAccountId = given_anMissingAccount();

        OpLogId opLogId = generateNextOperationIdFor(fromAccount);
        Decimal amount = amountBetween(d("0.01"), fromAccount.balance);
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccountId, amount);

        // Then
        expect_operationMarkedAsRejected(opLogId, "To Account '" + toAccountId + "' does not exist");

        // When
        handler.handleOperation(opLogId, new TransferFrom(randomOperationId(), randomOperationId(), transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountDoesNotExist() {
        AccountId fromAccountId = given_anMissingAccount();
        AccountId toAccountId = randomAccountId();

        OpLogId opLogId = randomOpLogId(fromAccountId);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccountId, toAccountId, amount);

        // Then
        expect_operationMarkedAsRejected(opLogId, "From Account '" + fromAccountId + "' does not exist");

        // When
        handler.handleOperation(opLogId, new TransferFrom(randomOperationId(), randomOperationId(), transferDetail));
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        Account toAccount = given_anAccountExists();

        OpLogId opLogId = generatePreviouslyAppliedOperationIdFor(fromAccount);
        Decimal amount = amountBetween(d("0.01"), fromAccount.balance);
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        // do nothing

        // When
        handler.handleOperation(opLogId, new TransferFrom(randomOperationId(), randomOperationId(), transferDetail));
    }

    private void expect_additionToWorkQueue(AccountId accountId) {
        doNothing().when(queue).add(accountId);
    }
}
