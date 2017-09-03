package mtymes.account.handler;

import mtymes.account.domain.operation.*;

public class HandlerDispatcher implements OperationVisitor<OperationHandler<?>> {

    private final CreateAccountHandler createAccountHandler;
    private final DepositToHandler depositToHandler;
    private final WithdrawFromHandler withdrawFromHandler;
    private final TransferFromHandler transferFromHandler;
    private final TransferToHandler transferToHandler;

    public HandlerDispatcher(CreateAccountHandler createAccountHandler, DepositToHandler depositToHandler, WithdrawFromHandler withdrawFromHandler, TransferFromHandler transferFromHandler, TransferToHandler transferToHandler) {
        this.createAccountHandler = createAccountHandler;
        this.depositToHandler = depositToHandler;
        this.withdrawFromHandler = withdrawFromHandler;
        this.transferFromHandler = transferFromHandler;
        this.transferToHandler = transferToHandler;
    }

    @Override
    public OperationHandler<CreateAccount> visit(CreateAccount request) {
        return createAccountHandler;
    }

    @Override
    public OperationHandler<DepositTo> visit(DepositTo request) {
        return depositToHandler;
    }

    @Override
    public OperationHandler<WithdrawFrom> visit(WithdrawFrom request) {
        return withdrawFromHandler;
    }

    @Override
    public OperationHandler<TransferFrom> visit(TransferFrom request) {
        return transferFromHandler;
    }

    @Override
    public OperationHandler<TransferTo> visit(TransferTo request) {
        return transferToHandler;
    }

    // todo: test this
    @SuppressWarnings("unchecked")
    public void dispatchOperation(LoggedOperation loggedOperation) {
        if (!loggedOperation.isFinished()) {
            Operation operation = loggedOperation.operation;
            OperationHandler handler = operation.apply(this);
            handler.handleOperation(loggedOperation.opLogId, operation);
        }
    }
}
