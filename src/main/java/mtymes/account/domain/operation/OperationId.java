package mtymes.account.domain.operation;

import javafixes.object.Microtype;

public class OperationId extends Microtype<Long> implements Comparable<OperationId> {

    private OperationId(long value) {
        super(value);
    }

    public static OperationId operationId(long value) {
        return new OperationId(value);
    }

    @Override
    public int compareTo(OperationId other) {
        return Long.compare(value(), other.value());
    }

    public boolean isBefore(OperationId operationId) {
        return this.compareTo(operationId) < 0;
    }

}
