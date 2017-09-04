package mtymes.account.handler;

import mtymes.account.domain.operation.*;

public class HandlerDispatcher {

    private final DispatchVisitor dispatchVisitor;

    public HandlerDispatcher(CreateAccountHandler createAccountHandler, DepositToHandler depositToHandler, WithdrawFromHandler withdrawFromHandler, TransferFromHandler transferFromHandler, TransferToHandler transferToHandler) {
        this.dispatchVisitor = new DispatchVisitor(createAccountHandler, depositToHandler, withdrawFromHandler, transferFromHandler, transferToHandler);
    }

    @SuppressWarnings("unchecked")
    public void dispatchOperation(LoggedOperation loggedOperation) {
        if (!loggedOperation.isFinished()) {
            Operation operation = loggedOperation.operation;
            OperationHandler handler = operation.apply(dispatchVisitor);
            handler.handleOperation(loggedOperation.opLogId, operation);
        }
    }

    private class DispatchVisitor implements OperationVisitor<OperationHandler<?>> {
        private final CreateAccountHandler createAccountHandler;
        private final DepositToHandler depositToHandler;
        private final WithdrawFromHandler withdrawFromHandler;
        private final TransferFromHandler transferFromHandler;
        private final TransferToHandler transferToHandler;

        private DispatchVisitor(CreateAccountHandler createAccountHandler, DepositToHandler depositToHandler, WithdrawFromHandler withdrawFromHandler, TransferFromHandler transferFromHandler, TransferToHandler transferToHandler) {
            this.createAccountHandler = createAccountHandler;
            this.depositToHandler = depositToHandler;
            this.withdrawFromHandler = withdrawFromHandler;
            this.transferFromHandler = transferFromHandler;
            this.transferToHandler = transferToHandler;
        }

        @Override
        public OperationHandler<CreateAccount> visit(CreateAccount operation) {
            return createAccountHandler;
        }

        @Override
        public OperationHandler<DepositTo> visit(DepositTo operation) {
            return depositToHandler;
        }

        @Override
        public OperationHandler<WithdrawFrom> visit(WithdrawFrom operation) {
            return withdrawFromHandler;
        }

        @Override
        public OperationHandler<TransferFrom> visit(TransferFrom operation) {
            return transferFromHandler;
        }

        @Override
        public OperationHandler<TransferTo> visit(TransferTo operation) {
            return transferToHandler;
        }
    }
}
