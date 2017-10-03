package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.dao.OpLogDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.exception.DuplicateItemException;
import mtymes.common.util.SetQueue;
import org.junit.Before;
import org.junit.Test;

import static javafixes.math.Decimal.ZERO;
import static javafixes.math.Decimal.d;
import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.*;

public class ReadableTransferFromHandlerTest extends ReadableOperationHandlerTest {

    private OpLogDao opLogDao;
    private SetQueue<AccountId> queue;
    private TransferFromHandler handler;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        opLogDao = mock(OpLogDao.class);
        queue = mock(SetQueue.class);
        handler = new TransferFromHandler(accountDao, operationDao, opLogDao, queue);
    }

    @Test
    public void shouldWithdrawMoneyAndQueueOperationTransferTo() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        Account toAccount = given_anAccountExists();

        OperationId fromPartOperationId = randomOperationId();
        OperationId toPartOperationId = randomOperationId();
        SeqId seqId = generateNextSeqIdFor(fromAccount);
        Decimal amount = amountBetween(d("0.01"), fromAccount.balance);
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_balanceUpdateOf(fromAccount, fromAccount.balance.minus(amount), seqId);
        expect_storageOf(new TransferTo(toPartOperationId, transferDetail));
        expect_additionToOpLog(toAccount.accountId, toPartOperationId);
        expect_additionToWorkQueue(toAccount.accountId);
        expect_operationMarkedAsApplied(fromPartOperationId);

        // When
        handler.handleOperation(seqId, new TransferFrom(fromPartOperationId, toPartOperationId, transferDetail));
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperationAndTransferToOperationAlreadyExists() {
        // Given
        Account fromAccount = given_anAccountExists();
        Account toAccount = given_anAccountExists();

        OperationId fromPartOperationId = randomOperationId();
        OperationId toPartOperationId = randomOperationId();
        SeqId seqId = generateCurrentlyAppliedSeqIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_storageOfDuplicate(new TransferTo(toPartOperationId, transferDetail));
        expect_additionToOpLog(toAccount.accountId, toPartOperationId);
        expect_additionToWorkQueue(toAccount.accountId);
        expect_operationMarkedAsApplied(fromPartOperationId);

        // When
        handler.handleOperation(seqId, new TransferFrom(fromPartOperationId, toPartOperationId, transferDetail));
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperationAndTransferToOperationAlreadyExistsAndIsAlreadyRegisteredInOpLog() {
        // Given
        Account fromAccount = given_anAccountExists();
        Account toAccount = given_anAccountExists();

        OperationId fromPartOperationId = randomOperationId();
        OperationId toPartOperationId = randomOperationId();
        SeqId seqId = generateCurrentlyAppliedSeqIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_storageOfDuplicate(new TransferTo(toPartOperationId, transferDetail));
        expect_additionOfDuplicateToOpLog(toAccount.accountId, toPartOperationId);
        expect_additionToWorkQueue(toAccount.accountId);
        expect_operationMarkedAsApplied(fromPartOperationId);

        // When
        handler.handleOperation(seqId, new TransferFrom(fromPartOperationId, toPartOperationId, transferDetail));
    }

    @Test
    public void shouldSucceedIfMoneyHasBeenAlreadyTransferredByThisOperation() {
        // Given
        Account fromAccount = given_anAccountExists();
        Account toAccount = given_anAccountExists();

        OperationId fromPartOperationId = randomOperationId();
        OperationId toPartOperationId = randomOperationId();
        SeqId seqId = generateCurrentlyAppliedSeqIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_storageOf(new TransferTo(toPartOperationId, transferDetail));
        expect_additionToWorkQueue(toAccount.accountId);
        expect_additionToOpLog(toAccount.accountId, toPartOperationId);
        expect_operationMarkedAsApplied(fromPartOperationId);

        // When
        handler.handleOperation(seqId, new TransferFrom(fromPartOperationId, toPartOperationId, transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountHasInsufficientFunds() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        Account toAccount = given_anAccountExists();

        OperationId fromPartOperationId = randomOperationId();
        SeqId seqId = generateNextSeqIdFor(fromAccount);
        Decimal amount = fromAccount.balance.plus(amountBetween(d("0.01"), d("1_000.00")));
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_operationMarkedAsRejected(fromPartOperationId, "Insufficient funds on account '" + fromAccount.accountId + "'");

        // When
        handler.handleOperation(seqId, new TransferFrom(fromPartOperationId, randomOperationId(), transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountHasZeroBalance() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(ZERO)
                .build());
        Account toAccount = given_anAccountExists();

        OperationId fromPartOperationId = randomOperationId();
        SeqId seqId = generateNextSeqIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_operationMarkedAsRejected(fromPartOperationId, "Insufficient funds on account '" + fromAccount.accountId + "'");

        // When
        handler.handleOperation(seqId, new TransferFrom(fromPartOperationId, randomOperationId(), transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountHasNegativeBalance() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("-10_000.00"), d("-0.01")))
                .build());
        Account toAccount = given_anAccountExists();

        OperationId fromPartOperationId = randomOperationId();
        SeqId seqId = generateNextSeqIdFor(fromAccount);
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        expect_operationMarkedAsRejected(fromPartOperationId, "Insufficient funds on account '" + fromAccount.accountId + "'");

        // When
        handler.handleOperation(seqId, new TransferFrom(fromPartOperationId, randomOperationId(), transferDetail));
    }

    @Test
    public void shouldFailIfToAccountDoesNotExist() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        AccountId toAccountId = given_anMissingAccount();

        OperationId fromPartOperationId = randomOperationId();
        SeqId seqId = generateNextSeqIdFor(fromAccount);
        Decimal amount = amountBetween(d("0.01"), fromAccount.balance);
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccountId, amount);

        // Then
        expect_operationMarkedAsRejected(fromPartOperationId, "To Account '" + toAccountId + "' does not exist");

        // When
        handler.handleOperation(seqId, new TransferFrom(fromPartOperationId, randomOperationId(), transferDetail));
    }

    @Test
    public void shouldFailIfFromAccountDoesNotExist() {
        AccountId fromAccountId = given_anMissingAccount();
        AccountId toAccountId = randomAccountId();

        OperationId fromPartOperationId = randomOperationId();
        SeqId seqId = randomSeqId();
        Decimal amount = randomPositiveAmount();
        TransferDetail transferDetail = generateTransferDetailFor(fromAccountId, toAccountId, amount);

        // Then
        expect_operationMarkedAsRejected(fromPartOperationId, "From Account '" + fromAccountId + "' does not exist");

        // When
        handler.handleOperation(seqId, new TransferFrom(fromPartOperationId, randomOperationId(), transferDetail));
    }

    @Test
    public void shouldDoNothingIfNextOperationIsAlreadyApplied() {
        // Given
        Account fromAccount = given_anAccountExists(accountBuilder()
                .balance(amountBetween(d("0.01"), d("10_000.00")))
                .build());
        Account toAccount = given_anAccountExists();

        SeqId seqId = generatePreviouslyAppliedSeqIdFor(fromAccount);
        Decimal amount = amountBetween(d("0.01"), fromAccount.balance);
        TransferDetail transferDetail = generateTransferDetailFor(fromAccount, toAccount, amount);

        // Then
        // do nothing

        // When
        handler.handleOperation(seqId, new TransferFrom(randomOperationId(), randomOperationId(), transferDetail));
    }

    private void expect_additionToOpLog(AccountId accountId, OperationId operationId) {
        when(opLogDao.registerOperationId(accountId, operationId)).thenReturn(randomSeqId());
    }

    private void expect_additionOfDuplicateToOpLog(AccountId accountId, OperationId operationId) {
        doThrow(new DuplicateItemException()).when(opLogDao).registerOperationId(accountId, operationId);
    }

    private void expect_additionToWorkQueue(AccountId accountId) {
        doNothing().when(queue).add(accountId);
    }
}
