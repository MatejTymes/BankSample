package mtymes.account.handler;

import mtymes.account.domain.operation.*;

public class HandlerDispatcher implements OperationVisitor<OperationHandler<?>> {

    private final CreateAccountHandler createAccountHandler;
    private final DepositMoneyHandler depositMoneyHandler;
    private final WithdrawMoneyHandler withdrawMoneyHandler;
    private final InternalTransferHandler internalTransferHandler;

    public HandlerDispatcher(CreateAccountHandler createAccountHandler, DepositMoneyHandler depositMoneyHandler, WithdrawMoneyHandler withdrawMoneyHandler, InternalTransferHandler internalTransferHandler) {
        this.createAccountHandler = createAccountHandler;
        this.depositMoneyHandler = depositMoneyHandler;
        this.withdrawMoneyHandler = withdrawMoneyHandler;
        this.internalTransferHandler = internalTransferHandler;
    }

    @Override
    public OperationHandler<CreateAccount> visit(CreateAccount request) {
        return createAccountHandler;
    }

    @Override
    public OperationHandler<DepositMoney> visit(DepositMoney request) {
        return depositMoneyHandler;
    }

    @Override
    public OperationHandler<WithdrawMoney> visit(WithdrawMoney request) {
        return withdrawMoneyHandler;
    }

    @Override
    public OperationHandler<InternalTransfer> visit(InternalTransfer request) {
        return internalTransferHandler;
    }
}
