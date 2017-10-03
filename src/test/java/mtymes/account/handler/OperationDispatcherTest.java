package mtymes.account.handler;

import mtymes.account.dao.OpLogDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static javafixes.common.CollectionUtil.newList;
import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.test.Random.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

public class OperationDispatcherTest extends StrictMockTest {

    private OpLogDao opLogDao;
    private CreateAccountHandler createAccountHandler;
    private DepositToHandler depositToHandler;
    private WithdrawFromHandler withdrawFromHandler;
    private TransferFromHandler transferFromHandler;
    private TransferToHandler transferToHandler;

    private OperationDispatcher operationDispatcher;

    @Before
    public void setUp() throws Exception {
        opLogDao = mock(OpLogDao.class);
        createAccountHandler = mock(CreateAccountHandler.class);
        depositToHandler = mock(DepositToHandler.class);
        withdrawFromHandler = mock(WithdrawFromHandler.class);
        transferFromHandler = mock(TransferFromHandler.class);
        transferToHandler = mock(TransferToHandler.class);
        operationDispatcher = new OperationDispatcher(opLogDao, createAccountHandler, depositToHandler, withdrawFromHandler, transferFromHandler, transferToHandler);
    }

    @Test
    public void shouldDispatchCreateAccount() {
        OperationId operationId = randomOperationId();
        SeqId seqId = randomSeqId();
        AccountId accountId = randomAccountId();
        CreateAccount operation = new CreateAccount(operationId, accountId);

        doNothing().when(createAccountHandler).handleOperation(seqId, operation);
        doNothing().when(opLogDao).markAsFinished(operationId);

        operationDispatcher.dispatchOperation(seqId, new LoggedOperation(operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchDepositTo() {
        OperationId operationId = randomOperationId();
        SeqId seqId = randomSeqId();
        AccountId accountId = randomAccountId();
        DepositTo operation = new DepositTo(operationId, accountId, randomPositiveAmount());

        doNothing().when(depositToHandler).handleOperation(seqId, operation);
        doNothing().when(opLogDao).markAsFinished(operationId);

        operationDispatcher.dispatchOperation(seqId, new LoggedOperation(operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchWithdrawFrom() {
        OperationId operationId = randomOperationId();
        SeqId seqId = randomSeqId();
        AccountId accountId = randomAccountId();
        WithdrawFrom operation = new WithdrawFrom(operationId, accountId, randomPositiveAmount());

        doNothing().when(withdrawFromHandler).handleOperation(seqId, operation);
        doNothing().when(opLogDao).markAsFinished(operationId);

        operationDispatcher.dispatchOperation(seqId, new LoggedOperation(operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchTransferFrom() {
        OperationId operationId = randomOperationId();
        OperationId toPartOperationId = randomOperationId();
        SeqId seqId = randomSeqId();
        TransferFrom operation = new TransferFrom(operationId, toPartOperationId, new TransferDetail(randomAccountId(), randomAccountId(), randomPositiveAmount()));

        doNothing().when(transferFromHandler).handleOperation(seqId, operation);
        doNothing().when(opLogDao).markAsFinished(operationId);

        operationDispatcher.dispatchOperation(seqId, new LoggedOperation(operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchTransferTo() {
        OperationId operationId = randomOperationId();
        SeqId seqId = randomSeqId();
        TransferTo operation = new TransferTo(operationId, new TransferDetail(randomAccountId(), randomAccountId(), randomPositiveAmount()));

        doNothing().when(transferToHandler).handleOperation(seqId, operation);
        doNothing().when(opLogDao).markAsFinished(operationId);

        operationDispatcher.dispatchOperation(seqId, new LoggedOperation(operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDoNothingIfOperationIsFinished() throws Exception {
        OperationId operationId = randomOperationId();
        SeqId seqId = randomSeqId();
        List<Operation> operations = newList(
                new CreateAccount(operationId, randomAccountId()),
                new DepositTo(operationId, randomAccountId(), randomPositiveAmount()),
                new WithdrawFrom(operationId, randomAccountId(), randomPositiveAmount()),
                new TransferFrom(operationId, randomOperationId(), new TransferDetail(randomAccountId(), randomAccountId(), randomPositiveAmount())),
                new TransferTo(operationId, new TransferDetail(randomAccountId(), randomAccountId(), randomPositiveAmount()))
        );

        for (Operation operation : operations) {
            doNothing().when(opLogDao).markAsFinished(operationId);

            operationDispatcher.dispatchOperation(
                    seqId,
                    new LoggedOperation(
                            operation,
                            Optional.of(Applied),
                            Optional.empty()
                    ));

            verifyMocksStrictly();

            doNothing().when(opLogDao).markAsFinished(operationId);

            operationDispatcher.dispatchOperation(
                    seqId,
                    new LoggedOperation(
                            operation,
                            Optional.of(Rejected),
                            Optional.of("Some rejection reason")
                    ));
        }
    }
}