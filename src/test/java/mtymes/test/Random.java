package mtymes.test;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.UUID.randomUUID;
import static javafixes.math.Decimal.decimal;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.account.domain.operation.OperationId.operationId;

// todo: move into test-infrastructure
public class Random {

    @SafeVarargs
    public static int randomInt(int from, int to, Condition<Integer>... validityConditions) {
        return generateValidValue(
                // typecast it to long as otherwise we could get int overflow
                () -> (int) ((long) (Math.random() * ((long) to - (long) from + 1L)) + (long) from),
                validityConditions
        );
    }

    @SafeVarargs
    public static long randomLong(long from, long to, Condition<Long>... validityConditions) {
        return generateValidValue(
                () -> ThreadLocalRandom.current().nextLong(from, to) + (long) randomInt(0, 1),
                validityConditions
        );
    }

    public static Decimal randomDecimal() {
        return decimal(
                randomLong(Long.MIN_VALUE, Long.MAX_VALUE),
                randomInt(-5, 5)
        );
    }

    public static Decimal randomPositiveDecimal() {
        return decimal(
                randomLong(1L, Long.MAX_VALUE),
                randomInt(-5, 5)
        );
    }

    public static AccountId randomAccountId() {
        return accountId(randomUUID());
    }

    @SafeVarargs
    public static OperationId randomOperationId(Condition<OperationId>... validityConditions) {
        return generateValidValue(
                () -> operationId(randomLong(0, Long.MAX_VALUE)),
                validityConditions
        );
    }

    @SafeVarargs
    public static <T> T pickRandomValue(T... values) {
        return values[randomInt(0, values.length - 1)];
    }

    public static <T> T pickRandomValue(List<T> values) {
        return values.get(randomInt(0, values.size() - 1));
    }

    @SafeVarargs
    private static <T> T generateValidValue(Supplier<T> generator, Condition<T>... validityConditions) {
        T value;

        int infiniteCycleCounter = 0;

        boolean valid;
        do {
            valid = true;
            value = generator.get();
            for (Function<T, Boolean> validityCondition : validityConditions) {
                if (!validityCondition.apply(value)) {
                    valid = false;
                    break;
                }
            }

            if (infiniteCycleCounter++ == 1_000) {
                throw new IllegalStateException("Possibly reached infinite cycle - unable to generate value after 1000 attempts.");
            }
        } while (!valid);

        return value;
    }
}
