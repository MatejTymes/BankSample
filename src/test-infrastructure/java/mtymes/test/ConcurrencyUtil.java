package mtymes.test;

import javafixes.concurrency.Runner;

import java.util.function.Consumer;

import static javafixes.concurrency.Runner.runner;

public class ConcurrencyUtil {

    public static void runConcurrentlyOnNThreads(Runnable task, int threadCount) {
        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                task.run();
            });
        }
        runner.waitTillDone().shutdown();
    }

    public static void runConcurrentlyOnNThreads(Consumer<Integer> task, int threadCount) {
        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                task.accept(index);
            });
        }
        runner.waitTillDone().shutdown();
    }
}
