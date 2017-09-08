package mtymes.common.util;

import java.util.concurrent.locks.StampedLock;

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
}
