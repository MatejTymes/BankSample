package mtymes.test;


import javafixes.math.Decimal;
import mtymes.account.domain.operation.OperationId;

import java.util.Set;
import java.util.function.Function;

import static javafixes.common.CollectionUtil.newSet;

// todo: move into test-infrastructure
public interface Condition<T> extends Function<T, Boolean> {

    @SafeVarargs
    static <T> Condition<T> otherThan(T... values) {
        Set<T> exclusions = newSet(values);
        return value -> !exclusions.contains(value);
    }

    static <T extends Number> Condition<T> positive() {
        return value -> signum(value) == 1;
    }

    static <T extends Number> Condition<T> negative() {
        return value -> signum(value) == -1;
    }

    static int signum(Number value) {
        if (value instanceof Integer) {
            return Integer.signum((Integer) value);
        } else if (value instanceof Long) {
            return Long.signum((Long) value);
        } else if (value instanceof Decimal) {
            return ((Decimal) value).signum();
        } else {
            throw new IllegalArgumentException("Unsupported number type: " + value.getClass());
        }
    }

    static Condition<OperationId> before(OperationId operationId) {
        return value -> value.compareTo(operationId) < 0;
    }

    static Condition<OperationId> after(OperationId operationId) {
        return value -> value.compareTo(operationId) > 0;
    }
}
