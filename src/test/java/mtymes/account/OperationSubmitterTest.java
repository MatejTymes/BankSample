package mtymes.account;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.work.Worker;
import mtymes.common.domain.Failure;
import mtymes.common.domain.Success;
import mtymes.common.util.Either;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.common.domain.Failure.failure;
import static mtymes.common.util.Either.left;
import static mtymes.common.util.Either.right;
import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class OperationSubmitterTest extends StrictMockTest {

    private IdGenerator idGenerator;
    private AccountDao accountDao;
    private OperationDao operationDao;
    private Worker worker;

    private OperationSubmitter submitter;

    @Before
    public void setUp() throws Exception {
        idGenerator = mock(IdGenerator.class);
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        worker = mock(Worker.class);

        submitter = new OperationSubmitter(idGenerator, accountDao, operationDao, worker);
    }

    @Test
    public void shouldCreateAccount() {
        AccountId accountId = randomAccountId();
        CreateAccount expectedOperation = new CreateAccount(accountId);
        OpLogId opLogId = randomOpLogId(accountId);
        Account expectedAccount = accountBuilder().accountId(accountId).build();

        when(idGenerator.nextAccountId()).thenReturn(accountId);
        when(operationDao.storeOperation(expectedOperation)).thenReturn(opLogId);
        doNothing().when(worker).runUnfinishedOperations(accountId);
        when(operationDao.findLoggedOperation(opLogId)).thenReturn(Optional.of(new LoggedOperation(opLogId, expectedOperation, Optional.of(Applied), Optional.empty())));
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(expectedAccount));

        // When
        Either<Failure, Account> response = submitter.createAccount();

        // Then
        assertThat(response, equalTo(right(expectedAccount)));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToLoadCreatedAccount() {
        AccountId accountId = randomAccountId();
        CreateAccount expectedOperation = new CreateAccount(accountId);
        OpLogId opLogId = randomOpLogId(accountId);

        when(idGenerator.nextAccountId()).thenReturn(accountId);
        when(operationDao.storeOperation(expectedOperation)).thenReturn(opLogId);
        doNothing().when(worker).runUnfinishedOperations(accountId);
        when(operationDao.findLoggedOperation(opLogId)).thenReturn(Optional.of(new LoggedOperation(opLogId, expectedOperation, Optional.of(Applied), Optional.empty())));
        when(accountDao.findAccount(accountId)).thenReturn(Optional.empty());

        // When
        Either<Failure, Account> response = submitter.createAccount();

        // Then
        assertThat(response, equalTo(left(failure("Failed to load created Account '" + accountId + "'"))));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToCreateAccount() {
        AccountId accountId = randomAccountId();
        CreateAccount expectedOperation = new CreateAccount(accountId);
        OpLogId opLogId = randomOpLogId(accountId);

        when(idGenerator.nextAccountId()).thenReturn(accountId);
        when(operationDao.storeOperation(expectedOperation)).thenReturn(opLogId);
        doNothing().when(worker).runUnfinishedOperations(accountId);
        String failureMessage = "for some reason the account was not created";
        when(operationDao.findLoggedOperation(opLogId)).thenReturn(Optional.of(new LoggedOperation(opLogId, expectedOperation, Optional.of(Rejected), Optional.of(failureMessage))));

        // When
        Either<Failure, Account> response = submitter.createAccount();

        // Then
        assertThat(response, equalTo(left(failure(failureMessage))));
    }

    @Test
    public void shouldDepositMoney() {
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        DepositTo expectedOperation = new DepositTo(accountId, amount);
        OpLogId opLogId = randomOpLogId(accountId);

        when(operationDao.storeOperation(expectedOperation)).thenReturn(opLogId);
        doNothing().when(worker).runUnfinishedOperations(accountId);
        when(operationDao.findLoggedOperation(opLogId)).thenReturn(Optional.of(new LoggedOperation(opLogId, expectedOperation, Optional.of(Applied), Optional.empty())));

        // When
        Either<Failure, Success> response = submitter.depositMoney(accountId, amount);

        // Then
        assertThat(response, equalTo(right(new Success())));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToDepositMoney() {
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        DepositTo expectedOperation = new DepositTo(accountId, amount);
        OpLogId opLogId = randomOpLogId(accountId);

        when(operationDao.storeOperation(expectedOperation)).thenReturn(opLogId);
        doNothing().when(worker).runUnfinishedOperations(accountId);
        String failureMessage = "for some reason the deposit of money failed";
        when(operationDao.findLoggedOperation(opLogId)).thenReturn(Optional.of(new LoggedOperation(opLogId, expectedOperation, Optional.of(Rejected), Optional.of(failureMessage))));

        // When
        Either<Failure, Success> response = submitter.depositMoney(accountId, amount);

        // Then
        assertThat(response, equalTo(left(failure(failureMessage))));
    }

    @Test
    public void shouldWithdrawMoney() {
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        WithdrawFrom expectedOperation = new WithdrawFrom(accountId, amount);
        OpLogId opLogId = randomOpLogId(accountId);

        when(operationDao.storeOperation(expectedOperation)).thenReturn(opLogId);
        doNothing().when(worker).runUnfinishedOperations(accountId);
        when(operationDao.findLoggedOperation(opLogId)).thenReturn(Optional.of(new LoggedOperation(opLogId, expectedOperation, Optional.of(Applied), Optional.empty())));

        // When
        Either<Failure, Success> response = submitter.withdrawMoney(accountId, amount);

        // Then
        assertThat(response, equalTo(right(new Success())));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToWithdrawMoney() {
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        WithdrawFrom expectedOperation = new WithdrawFrom(accountId, amount);
        OpLogId opLogId = randomOpLogId(accountId);

        when(operationDao.storeOperation(expectedOperation)).thenReturn(opLogId);
        doNothing().when(worker).runUnfinishedOperations(accountId);
        String failureMessage = "for some reason the withdraw failed";
        when(operationDao.findLoggedOperation(opLogId)).thenReturn(Optional.of(new LoggedOperation(opLogId, expectedOperation, Optional.of(Rejected), Optional.of(failureMessage))));

        // When
        Either<Failure, Success> response = submitter.withdrawMoney(accountId, amount);

        // Then
        assertThat(response, equalTo(left(failure(failureMessage))));
    }

    @Test
    public void shouldTransferMoney() {
        TransferId transferId = randomTransferId();
        AccountId fromAccountId = randomAccountId();
        AccountId toAccountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        TransferFrom expectedOperation = new TransferFrom(new TransferDetail(transferId, fromAccountId, toAccountId, amount));
        OpLogId opLogId = randomOpLogId(fromAccountId);

        when(idGenerator.nextTransferId()).thenReturn(transferId);
        when(operationDao.storeOperation(expectedOperation)).thenReturn(opLogId);
        doNothing().when(worker).runUnfinishedOperations(fromAccountId);
        when(operationDao.findLoggedOperation(opLogId)).thenReturn(Optional.of(new LoggedOperation(opLogId, expectedOperation, Optional.of(Applied), Optional.empty())));

        // When
        Either<Failure, Success> response = submitter.transferMoney(fromAccountId, toAccountId, amount);

        // Then
        assertThat(response, equalTo(right(new Success())));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToTransferMoney() {
        TransferId transferId = randomTransferId();
        AccountId fromAccountId = randomAccountId();
        AccountId toAccountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        TransferFrom expectedOperation = new TransferFrom(new TransferDetail(transferId, fromAccountId, toAccountId, amount));
        OpLogId opLogId = randomOpLogId(fromAccountId);

        when(idGenerator.nextTransferId()).thenReturn(transferId);
        when(operationDao.storeOperation(expectedOperation)).thenReturn(opLogId);
        doNothing().when(worker).runUnfinishedOperations(fromAccountId);
        String failureMessage = "for some reason the transfer failed";
        when(operationDao.findLoggedOperation(opLogId)).thenReturn(Optional.of(new LoggedOperation(opLogId, expectedOperation, Optional.of(Rejected), Optional.of(failureMessage))));

        // When
        Either<Failure, Success> response = submitter.transferMoney(fromAccountId, toAccountId, amount);

        // Then
        assertThat(response, equalTo(left(failure(failureMessage))));
    }
}