package mtymes.common.util;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

// todo: test this
public class Locker {

    private final StampedLock lock = new StampedLock();

    public void lockAndRun(Runnable task) {
        long stamp = lock.writeLock();
        try {
            task.run();
        } finally {
            lock.unlock(stamp);
        }
    }

    public <T> T lockAndRun(Supplier<T> task) {
        long stamp = lock.writeLock();
        try {
            return task.get();
        } finally {
            lock.unlock(stamp);
        }
    }
}
