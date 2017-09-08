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
        OpLogId opLogId = randomOpLogId();
        CreateAccount operation = new CreateAccount(opLogId.accountId);

        doNothing().when(createAccountHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchDepositTo() {
        OpLogId opLogId = randomOpLogId();
        DepositTo operation = new DepositTo(opLogId.accountId, randomPositiveAmount());

        doNothing().when(depositToHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchWithdrawFrom() {
        OpLogId opLogId = randomOpLogId();
        WithdrawFrom operation = new WithdrawFrom(opLogId.accountId, randomPositiveAmount());

        doNothing().when(withdrawFromHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchTransferFrom() {
        OpLogId opLogId = randomOpLogId();
        TransferFrom operation = new TransferFrom(new TransferDetail(randomTransferId(), opLogId.accountId, randomAccountId(), randomPositiveAmount()));

        doNothing().when(transferFromHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDispatchTransferTo() {
        OpLogId opLogId = randomOpLogId();
        TransferTo operation = new TransferTo(new TransferDetail(randomTransferId(), randomAccountId(), opLogId.accountId, randomPositiveAmount()));

        doNothing().when(transferToHandler).handleOperation(opLogId, operation);

        operationDispatcher.dispatchOperation(new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty()));
    }

    @Test
    public void shouldDoNothingIfOperationIsFinished() {
        OpLogId opLogId = randomOpLogId();
        List<Operation> operations = newList(
                new CreateAccount(opLogId.accountId),
                new DepositTo(opLogId.accountId, randomPositiveAmount()),
                new WithdrawFrom(opLogId.accountId, randomPositiveAmount()),
                new TransferFrom(new TransferDetail(randomTransferId(), opLogId.accountId, randomAccountId(), randomPositiveAmount())),
                new TransferTo(new TransferDetail(randomTransferId(), randomAccountId(), opLogId.accountId, randomPositiveAmount()))
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