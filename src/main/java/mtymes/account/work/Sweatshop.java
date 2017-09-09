package mtymes.account.work;

import javafixes.concurrency.Runner;
import mtymes.account.domain.QueuedWorkStats;
import mtymes.account.domain.account.AccountId;
import mtymes.common.util.Locker;
import mtymes.common.util.SetQueue;

import java.time.Duration;
import java.util.List;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static javafixes.concurrency.Runner.runner;

// todo: test this
public class Sweatshop {

    private final Locker locker = new Locker();

    private final SetQueue<AccountId> workQueue;
    private final int workerCount;
    private final Worker worker;
    private final Duration timeoutIfNoWork;

    private volatile Runner runner;
    private final List<WorkerThread> workers = newCopyOnWriteArrayList();

    public Sweatshop(SetQueue<AccountId> workQueue, int workerCount, Worker worker, Duration timeoutIfNoWork) {
        this.workQueue = workQueue;
        this.workerCount = workerCount;
        this.worker = worker;
        this.timeoutIfNoWork = timeoutIfNoWork;
    }

    // todo: test this
    public Object queuedWorkStats() {
        return new QueuedWorkStats(
                workQueue.size(),
                (int) workers.stream().filter(WorkerThread::isWorking).count()
        );
    }

    public Sweatshop start() {
        locker.lockAndRun(() -> {
            runner = runner(workerCount);
            for (int i = 0; i < workerCount; i++) {
                WorkerThread workerThread = new WorkerThread(workQueue, worker, timeoutIfNoWork);
                runner.run(workerThread);
                workers.add(workerThread);
            }
        });
        return this;
    }

    public void shutdown() {
        locker.lockAndRun(() -> {
            runner.shutdown();
            workers.clear();
        });
    }
}
