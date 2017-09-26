package mtymes.test;

import javafixes.math.Decimal;
import javafixes.math.Scale;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
import mtymes.account.domain.operation.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.math.BigDecimal.ROUND_DOWN;
import static java.util.UUID.randomUUID;
import static javafixes.math.Decimal.decimal;
import static javafixes.math.Scale._2_DECIMAL_PLACES;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.account.domain.account.Version.version;
import static mtymes.account.domain.operation.OpLogId.opLogId;
import static mtymes.account.domain.operation.OperationId.operationId;
import static mtymes.account.domain.operation.TransferId.transferId;

public class Random {

    private static final Scale scaleToUse = _2_DECIMAL_PLACES;

    public static boolean randomBoolean() {
        return pickRandomValue(true, false);
    }

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

    public static String randomUUIDString() {
        return randomUUID().toString();
    }

    public static Decimal randomAmount() {
        return decimal(
                randomInt(Integer.MIN_VALUE, Integer.MAX_VALUE),
                scaleToUse.value
        );
    }

    public static Decimal randomPositiveAmount() {
        return decimal(
                randomInt(1, Integer.MAX_VALUE),
                scaleToUse.value
        );
    }

    public static Decimal randomNegativeAmount() {
        return decimal(
                randomInt(Integer.MIN_VALUE, -1),
                scaleToUse.value
        );
    }

    public static Decimal randomAmountBetween(Decimal fromAmount, Decimal toAmount) {
        long fromLong = fromAmount.bigDecimalValue().setScale(scaleToUse.value, ROUND_DOWN).unscaledValue().longValue();
        long toLong = toAmount.bigDecimalValue().setScale(scaleToUse.value, ROUND_DOWN).unscaledValue().longValue();
        return decimal(randomLong(fromLong, toLong), scaleToUse.value);
    }

    public static OperationId randomOperationId() {
        return operationId(randomUUID());
    }

    public static AccountId randomAccountId() {
        return accountId(randomUUID());
    }

    public static TransferId randomTransferId() {
        return transferId(randomUUID());
    }

    @SafeVarargs
    public static Version randomVersion(Condition<Version>... validityConditions) {
        return generateValidValue(
                () -> version(randomInt(0, Integer.MAX_VALUE)),
                validityConditions
        );
    }

    @SafeVarargs
    public static OpLogId randomOpLogId(AccountId accountId, Condition<OpLogId>... validityConditions) {
        return generateValidValue(
                () -> opLogId(accountId, randomVersion()),
                validityConditions
        );
    }

    public static OpLogId randomOpLogId() {
        return randomOpLogId(randomAccountId());
    }

    public static Operation randomOperation(AccountId accountId) {
        switch (randomInt(1, 5)) {
            case 1:
                return new CreateAccount(randomOperationId(), accountId);
            case 2:
                return new DepositTo(randomOperationId(), accountId, randomPositiveAmount());
            case 3:
                return new WithdrawFrom(accountId, randomPositiveAmount());
            case 4:
                return new TransferFrom(new TransferDetail(randomTransferId(), accountId, randomAccountId(), randomPositiveAmount()));
            default:
                return new TransferTo(new TransferDetail(randomTransferId(), randomAccountId(), accountId, randomPositiveAmount()));
        }
    }

    public static Operation randomOperation() {
        return randomOperation(randomAccountId());
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
