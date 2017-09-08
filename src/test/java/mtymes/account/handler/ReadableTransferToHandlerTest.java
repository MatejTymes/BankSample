package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferTo;
import org.junit.Before;
import org.junit.Test;

import static mtymes.test.Random.*;

public class ReadableTransferToHandlerTest extends ReadableOperationHandlerTest {

    private TransferToHandler handler;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new TransferToHandler(accountDao, operationDao);
    }

    @Test
    public void shouldDepositMoney() {
        // Given
        Account toAccount = given_anAccountExists();

        OpLogId opLogId = generateNextOperationIdFor(toAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(randomAccountId(), toAccount, amount);

        // Then
        expect_balanceUpdateOf(toAccount, toAccount.balance.plus(amount), opLogId);
        expect_operationMarkedAsApplied(opLogId);

        // When
        handler.handleOperation(opLogId, new TransferTo(transferDetail));
    }

    @Test
    public void shouldSucceedIfBalanceHasBeenAlreadyUpdatedByThisOperation() {
        // Given
        Account toAccount = given_anAccountExists();

        OpLogId opLogId = generateCurrentlyAppliedOperationIdFor(toAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(randomAccountId(), toAccount, amount);

        // Then
        expect_operationMarkedAsApplied(opLogId);

        // When
        handler.handleOperation(opLogId, new TransferTo(transferDetail));
    }

    @Test
    public void shouldFailIfAccountDoesNotExist() {
        // Given
        AccountId toAccountId = given_anMissingAccount();

        OpLogId opLogId = randomOpLogId(toAccountId);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(randomAccountId(), toAccountId, amount);

        // Then
        expect_operationMarkedAsRejected(opLogId, "To Account '" + toAccountId + "' does not exist");

        // When
        handler.handleOperation(opLogId, new TransferTo(transferDetail));
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        // Given
        Account toAccount = given_anAccountExists();

        OpLogId opLogId = generatePreviouslyAppliedOperationIdFor(toAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(randomAccountId(), toAccount, amount);

        // Then
        // do nothing

        // When
        handler.handleOperation(opLogId, new TransferTo(transferDetail));
    }
}