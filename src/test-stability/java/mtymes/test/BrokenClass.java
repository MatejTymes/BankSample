package mtymes.test;

import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.collect.Sets.newHashSet;

abstract class BrokenClass {

    private final Set<MethodCall> alreadyFailedCalls = newHashSet();

    private final Supplier<RuntimeException> exceptionSupplier;

    protected BrokenClass(Supplier<RuntimeException> exceptionSupplier) {
        this.exceptionSupplier = exceptionSupplier;
    }

    protected void failTheFirstTime(String methodName, Object... params) throws RuntimeException {
        MethodCall call = new MethodCall(methodName, params);
        if (!alreadyFailedCalls.contains(call)) {
            alreadyFailedCalls.add(call);
            throw exceptionSupplier.get();
        }
    }
}
