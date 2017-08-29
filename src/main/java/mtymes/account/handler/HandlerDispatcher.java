package mtymes.account.handler;

import mtymes.account.domain.operation.*;

public class HandlerDispatcher implements OperationVisitor<OperationHandler<?>> {

    private final CreateAccountHandler createAccountHandler;
    private final DepositMoneyHandler depositMoneyHandler;
    private final WithdrawMoneyHandler withdrawMoneyHandler;
    private final TransferMoneyFromHandler transferMoneyFromHandler;
    private final TransferMoneyToHandler transferMoneyToHandler;

    public HandlerDispatcher(CreateAccountHandler createAccountHandler, DepositMoneyHandler depositMoneyHandler, WithdrawMoneyHandler withdrawMoneyHandler, TransferMoneyFromHandler transferMoneyFromHandler, TransferMoneyToHandler transferMoneyToHandler) {
        this.createAccountHandler = createAccountHandler;
        this.depositMoneyHandler = depositMoneyHandler;
        this.withdrawMoneyHandler = withdrawMoneyHandler;
        this.transferMoneyFromHandler = transferMoneyFromHandler;
        this.transferMoneyToHandler = transferMoneyToHandler;
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
    public OperationHandler<TransferMoneyFrom> visit(TransferMoneyFrom request) {
        return transferMoneyFromHandler;
    }

    @Override
    public OperationHandler<TransferMoneyTo> visit(TransferMoneyTo request) {
        return transferMoneyToHandler;
    }
}
