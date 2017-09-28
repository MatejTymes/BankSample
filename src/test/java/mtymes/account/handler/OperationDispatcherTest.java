package mtymes.account.handler;

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

    private CreateAccountHandler createAccountHandler;
    private DepositToHandler depositToHandler;
    private WithdrawFromHandler withdrawFromHandler;
    private TransferFromHandler transferFromHandler;
    private TransferToHandler transferToHandler;

    private OperationDispatcher operationDispatcher;

    @Before
    public void setUp() throws Exception {
        createAccountHandler = mock(CreateAccountHandler.class);
        depositToHandler = mock(DepositToHandler.class);
        withdrawFromHandler = mock(WithdrawFromHandler.class);
        transferFromHandler = mock(TransferFromHandler.class);
        transferToHandler = mock(TransferToHandler.class);
        operationDispatcher = new OperationDispatcher(createAccountHandler, depositToHandler, withdrawFromHandler, transferFromHandler, transferToHandler);
    }

    @Test
    public void shouldDispatchCreateAccount() {
        OperationId operationId = randomOperationId();
        OpLogId opLogId = randomOpLogId();
        CreateAccount operation = new CreateAccount(operationId, opLogId.accountId);

        doNothing().when(createAccountHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchDepositTo() {
        OperationId operationId = randomOperationId();
        OpLogId opLogId = randomOpLogId();
        DepositTo operation = new DepositTo(operationId, opLogId.accountId, randomPositiveAmount());

        doNothing().when(depositToHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchWithdrawFrom() {
        OperationId operationId = randomOperationId();
        OpLogId opLogId = randomOpLogId();
        WithdrawFrom operation = new WithdrawFrom(operationId, opLogId.accountId, randomPositiveAmount());

        doNothing().when(withdrawFromHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchTransferFrom() {
        OperationId operationId = randomOperationId();
        OperationId toPartOperationId = randomOperationId();
        OpLogId opLogId = randomOpLogId();
        TransferFrom operation = new TransferFrom(operationId, toPartOperationId, new TransferDetail(opLogId.accountId, randomAccountId(), randomPositiveAmount()));

        doNothing().when(transferFromHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchTransferTo() {
        OperationId operationId = randomOperationId();
        OpLogId opLogId = randomOpLogId();
        TransferTo operation = new TransferTo(operationId, new TransferDetail(randomAccountId(), opLogId.accountId, randomPositiveAmount()));

        doNothing().when(transferToHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDoNothingIfOperationIsFinished() {
        OperationId operationId = randomOperationId();
        OpLogId opLogId = randomOpLogId();
        List<Operation> operations = newList(
                new CreateAccount(operationId, opLogId.accountId),
                new DepositTo(operationId, opLogId.accountId, randomPositiveAmount()),
                new WithdrawFrom(operationId, opLogId.accountId, randomPositiveAmount()),
                new TransferFrom(operationId, randomOperationId(), new TransferDetail(opLogId.accountId, randomAccountId(), randomPositiveAmount())),
                new TransferTo(operationId, new TransferDetail(randomAccountId(), opLogId.accountId, randomPositiveAmount()))
        );

        for (Operation operation : operations) {
            operationDispatcher.dispatchOperation(
                    new LoggedOperation(
                            opLogId,
                            operation,
                            Optional.of(Applied),
                            Optional.empty()
                    ));
            operationDispatcher.dispatchOperation(
                    new LoggedOperation(
                            opLogId,
                            operation,
                            Optional.of(Rejected),
                            Optional.of("Some rejection reason")
                    ));
        }
    }

}