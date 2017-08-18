package mtymes.test;

import java.util.concurrent.CountDownLatch;

public class ThreadSynchronizer {

    private final CountDownLatch latch;

    public ThreadSynchronizer(int threadCount) {
        latch = new CountDownLatch(threadCount);
    }

    public void synchronizeThreadsAtThisPoint() throws InterruptedException {
        latch.countDown();
        latch.await();
    }
}
