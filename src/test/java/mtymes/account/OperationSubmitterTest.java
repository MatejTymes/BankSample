package mtymes.account;

import javafixes.math.Decimal;
import javafixes.object.Either;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OpLogDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.work.Worker;
import mtymes.common.domain.Failure;
import mtymes.common.domain.Success;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static javafixes.object.Either.left;
import static javafixes.object.Either.right;
import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.common.domain.Failure.failure;
import static mtymes.domain.account.AccountBuilder.accountBuilder;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class OperationSubmitterTest extends StrictMockTest {

    private IdGenerator idGenerator;
    private AccountDao accountDao;
    private OperationDao operationDao;
    private OpLogDao opLogDao;
    private Worker worker;

    private OperationSubmitter submitter;

    @Before
    public void setUp() throws Exception {
        idGenerator = mock(IdGenerator.class);
        accountDao = mock(AccountDao.class);
        operationDao = mock(OperationDao.class);
        opLogDao = mock(OpLogDao.class);
        worker = mock(Worker.class);

        submitter = new OperationSubmitter(idGenerator, accountDao, operationDao, opLogDao, worker);
    }

    @Test
    public void shouldCreateAccount() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        CreateAccount expectedOperation = new CreateAccount(operationId, accountId);
        Account expectedAccount = accountBuilder().accountId(accountId).build();

        when(idGenerator.nextOperationId()).thenReturn(operationId);
        when(idGenerator.nextAccountId()).thenReturn(accountId);
        doNothing().when(operationDao).storeOperation(expectedOperation);
        when(opLogDao.registerOperationId(accountId, operationId)).thenReturn(randomSeqId());
        doNothing().when(worker).runUnfinishedOperations(accountId);
        when(operationDao.findLoggedOperation(operationId)).thenReturn(Optional.of(new LoggedOperation(expectedOperation, Optional.of(Applied), Optional.empty())));
        when(accountDao.findAccount(accountId)).thenReturn(Optional.of(expectedAccount));

        // When
        Either<Failure, Account> response = submitter.createAccount();

        // Then
        assertThat(response, equalTo(right(expectedAccount)));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToLoadCreatedAccount() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        CreateAccount expectedOperation = new CreateAccount(operationId, accountId);

        when(idGenerator.nextOperationId()).thenReturn(operationId);
        when(idGenerator.nextAccountId()).thenReturn(accountId);
        doNothing().when(operationDao).storeOperation(expectedOperation);
        when(opLogDao.registerOperationId(accountId, operationId)).thenReturn(randomSeqId());
        doNothing().when(worker).runUnfinishedOperations(accountId);
        when(operationDao.findLoggedOperation(operationId)).thenReturn(Optional.of(new LoggedOperation(expectedOperation, Optional.of(Applied), Optional.empty())));
        when(accountDao.findAccount(accountId)).thenReturn(Optional.empty());

        // When
        Either<Failure, Account> response = submitter.createAccount();

        // Then
        assertThat(response, equalTo(left(failure("Failed to load created Account '" + accountId + "'"))));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToCreateAccount() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        CreateAccount expectedOperation = new CreateAccount(operationId, accountId);

        when(idGenerator.nextOperationId()).thenReturn(operationId);
        when(idGenerator.nextAccountId()).thenReturn(accountId);
        doNothing().when(operationDao).storeOperation(expectedOperation);
        when(opLogDao.registerOperationId(accountId, operationId)).thenReturn(randomSeqId());
        doNothing().when(worker).runUnfinishedOperations(accountId);
        String failureMessage = "for some reason the account was not created";
        when(operationDao.findLoggedOperation(operationId)).thenReturn(Optional.of(new LoggedOperation(expectedOperation, Optional.of(Rejected), Optional.of(failureMessage))));

        // When
        Either<Failure, Account> response = submitter.createAccount();

        // Then
        assertThat(response, equalTo(left(failure(failureMessage))));
    }

    @Test
    public void shouldDepositMoney() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        DepositTo expectedOperation = new DepositTo(operationId, accountId, amount);

        when(idGenerator.nextOperationId()).thenReturn(operationId);
        doNothing().when(operationDao).storeOperation(expectedOperation);
        when(opLogDao.registerOperationId(accountId, operationId)).thenReturn(randomSeqId());
        doNothing().when(worker).runUnfinishedOperations(accountId);
        when(operationDao.findLoggedOperation(operationId)).thenReturn(Optional.of(new LoggedOperation(expectedOperation, Optional.of(Applied), Optional.empty())));

        // When
        Either<Failure, Success> response = submitter.depositMoney(accountId, amount);

        // Then
        assertThat(response, equalTo(right(new Success())));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToDepositMoney() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        DepositTo expectedOperation = new DepositTo(operationId, accountId, amount);

        when(idGenerator.nextOperationId()).thenReturn(operationId);
        doNothing().when(operationDao).storeOperation(expectedOperation);
        when(opLogDao.registerOperationId(accountId, operationId)).thenReturn(randomSeqId());
        doNothing().when(worker).runUnfinishedOperations(accountId);
        String failureMessage = "for some reason the deposit of money failed";
        when(operationDao.findLoggedOperation(operationId)).thenReturn(Optional.of(new LoggedOperation(expectedOperation, Optional.of(Rejected), Optional.of(failureMessage))));

        // When
        Either<Failure, Success> response = submitter.depositMoney(accountId, amount);

        // Then
        assertThat(response, equalTo(left(failure(failureMessage))));
    }

    @Test
    public void shouldWithdrawMoney() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        WithdrawFrom expectedOperation = new WithdrawFrom(operationId, accountId, amount);

        when(idGenerator.nextOperationId()).thenReturn(operationId);
        doNothing().when(operationDao).storeOperation(expectedOperation);
        when(opLogDao.registerOperationId(accountId, operationId)).thenReturn(randomSeqId());
        doNothing().when(worker).runUnfinishedOperations(accountId);
        when(operationDao.findLoggedOperation(operationId)).thenReturn(Optional.of(new LoggedOperation(expectedOperation, Optional.of(Applied), Optional.empty())));

        // When
        Either<Failure, Success> response = submitter.withdrawMoney(accountId, amount);

        // Then
        assertThat(response, equalTo(right(new Success())));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToWithdrawMoney() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        WithdrawFrom expectedOperation = new WithdrawFrom(operationId, accountId, amount);

        when(idGenerator.nextOperationId()).thenReturn(operationId);
        doNothing().when(operationDao).storeOperation(expectedOperation);
        when(opLogDao.registerOperationId(accountId, operationId)).thenReturn(randomSeqId());
        doNothing().when(worker).runUnfinishedOperations(accountId);
        String failureMessage = "for some reason the withdraw failed";
        when(operationDao.findLoggedOperation(operationId)).thenReturn(Optional.of(new LoggedOperation(expectedOperation, Optional.of(Rejected), Optional.of(failureMessage))));

        // When
        Either<Failure, Success> response = submitter.withdrawMoney(accountId, amount);

        // Then
        assertThat(response, equalTo(left(failure(failureMessage))));
    }

    @Test
    public void shouldTransferMoney() {
        OperationId operationId = randomOperationId();
        OperationId toPartOperationId = randomOperationId();
        AccountId fromAccountId = randomAccountId();
        AccountId toAccountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        TransferFrom expectedOperation = new TransferFrom(operationId, toPartOperationId, new TransferDetail(fromAccountId, toAccountId, amount));

        when(idGenerator.nextOperationId()).thenReturn(operationId, toPartOperationId);
        doNothing().when(operationDao).storeOperation(expectedOperation);
        when(opLogDao.registerOperationId(fromAccountId, operationId)).thenReturn(randomSeqId());
        doNothing().when(worker).runUnfinishedOperations(fromAccountId);
        when(operationDao.findLoggedOperation(operationId)).thenReturn(Optional.of(new LoggedOperation(expectedOperation, Optional.of(Applied), Optional.empty())));

        // When
        Either<Failure, Success> response = submitter.transferMoney(fromAccountId, toAccountId, amount);

        // Then
        assertThat(response, equalTo(right(new Success())));
    }

    @Test
    public void shouldReceiveFailureMessageIfUnableToTransferMoney() {
        OperationId operationId = randomOperationId();
        OperationId toPartOperationId = randomOperationId();
        AccountId fromAccountId = randomAccountId();
        AccountId toAccountId = randomAccountId();
        Decimal amount = randomPositiveAmount();
        TransferFrom expectedOperation = new TransferFrom(operationId, toPartOperationId, new TransferDetail(fromAccountId, toAccountId, amount));

        when(idGenerator.nextOperationId()).thenReturn(operationId, toPartOperationId);
        doNothing().when(operationDao).storeOperation(expectedOperation);
        when(opLogDao.registerOperationId(fromAccountId, operationId)).thenReturn(randomSeqId());
        doNothing().when(worker).runUnfinishedOperations(fromAccountId);
        String failureMessage = "for some reason the transfer failed";
        when(operationDao.findLoggedOperation(operationId)).thenReturn(Optional.of(new LoggedOperation(expectedOperation, Optional.of(Rejected), Optional.of(failureMessage))));

        // When
        Either<Failure, Success> response = submitter.transferMoney(fromAccountId, toAccountId, amount);

        // Then
        assertThat(response, equalTo(left(failure(failureMessage))));
    }
}