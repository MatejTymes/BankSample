package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
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

        OperationId operationId = randomOperationId();
        SeqId seqId = generateNextSeqIdFor(toAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(randomAccountId(), toAccount, amount);

        // Then
        expect_balanceUpdateOf(toAccount, toAccount.balance.plus(amount), seqId);
        expect_operationMarkedAsApplied(operationId);

        // When
        handler.handleOperation(seqId, new TransferTo(operationId, transferDetail));
    }

    @Test
    public void shouldSucceedIfBalanceHasBeenAlreadyUpdatedByThisOperation() {
        // Given
        Account toAccount = given_anAccountExists();

        OperationId operationId = randomOperationId();
        SeqId seqId = generateCurrentlyAppliedSeqIdFor(toAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(randomAccountId(), toAccount, amount);

        // Then
        expect_operationMarkedAsApplied(operationId);

        // When
        handler.handleOperation(seqId, new TransferTo(operationId, transferDetail));
    }

    @Test
    public void shouldFailIfAccountDoesNotExist() {
        // Given
        AccountId toAccountId = given_anMissingAccount();

        OperationId operationId = randomOperationId();
        SeqId seqId = randomSeqId();
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(randomAccountId(), toAccountId, amount);

        // Then
        expect_operationMarkedAsRejected(operationId, "To Account '" + toAccountId + "' does not exist");

        // When
        handler.handleOperation(seqId, new TransferTo(operationId, transferDetail));
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        // Given
        Account toAccount = given_anAccountExists();

        OperationId operationId = randomOperationId();
        SeqId seqId = generatePreviouslyAppliedSeqIdFor(toAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(randomAccountId(), toAccount, amount);

        // Then
        // do nothing

        // When
        handler.handleOperation(seqId, new TransferTo(operationId, transferDetail));
    }
}