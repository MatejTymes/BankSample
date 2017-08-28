package mtymes.account.domain.operation;

public interface OperationVisitor<T> {

    T visit(CreateAccount request);

    T visit(DepositMoney request);

    T visit(WithdrawMoney request);

    @Deprecated // todo: remove
    T visit(InternalTransfer request);

    T visit(TransferMoneyFrom request);

    T visit(TransferMoneyTo request);
}
