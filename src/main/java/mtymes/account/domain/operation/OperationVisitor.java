package mtymes.account.domain.operation;

public interface OperationVisitor<T> {

    T visit(CreateAccount request);

    T visit(DepositTo request);

    T visit(WithdrawFrom request);

    T visit(TransferFrom request);

    T visit(TransferTo request);
}
