package mtymes.account.handler;

import javafixes.concurrency.Runner;
import mtymes.test.ThreadSynchronizer;

import static javafixes.concurrency.Runner.runner;

public class BaseOperationHandlerConcurrencyTest extends BaseOperationHandlerStabilityTest {

    // todo: start using this
    public void runConcurrentlyOnNThreads(Runnable task, int threadCount) {
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
}
