package mtymes.account.work;

import javafixes.concurrency.Task;
import mtymes.account.domain.account.AccountId;
import mtymes.common.util.SetQueue;
import mtymes.common.util.Sleeper;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Optional;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

public class WorkerThread implements Task {

    private Logger logger = getLogger(WorkerThread.class);

    private final SetQueue<AccountId> queue;
    private final Worker worker;
    private final Sleeper sleeper;
    private final Duration timeoutIfNoWork;

    private volatile Optional<AccountId> workInProgress = Optional.empty();

    public WorkerThread(SetQueue<AccountId> queue, Worker worker, Sleeper sleeper, Duration timeoutIfNoWork) {
        this.queue = queue;
        this.worker = worker;
        this.sleeper = sleeper;
        this.timeoutIfNoWork = timeoutIfNoWork;
    }

    public void run() throws InterruptedException {
        while (true) {
            Optional<AccountId> optionalAccountId = queue.takeNextAvailable();

            if (optionalAccountId.isPresent()) {
                processAccountOperations(optionalAccountId.get());
                sleeper.sleepFor(0L); // allow interruption
            } else {
                sleeper.sleepFor(timeoutIfNoWork); // allow interruption
            }
        }
    }

    public boolean isWorking() {
        return workInProgress.isPresent();
    }

    private void processAccountOperations(AccountId accountId) {
        try {
            workInProgress = Optional.of(accountId);

            worker.runUnfinishedOperations(accountId);
        } catch (RuntimeException e) {
            logger.error(format("Failed to evaluate operation for Account '%s'", accountId), e);
            queue.add(accountId);
        } finally {
            workInProgress = Optional.empty();
        }
    }
}
