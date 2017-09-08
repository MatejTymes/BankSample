package mtymes.account.work;

import javafixes.concurrency.Task;
import mtymes.account.domain.account.AccountId;
import mtymes.common.util.SetQueue;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Optional;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

// todo: test this
public class WorkerThread implements Task {

    private Logger logger = getLogger(WorkerThread.class);

    private final SetQueue queue;
    private final Worker worker;
    private final Duration timeoutIfNoWork;

    private volatile Optional<AccountId> workInProgress = Optional.empty();

    public WorkerThread(SetQueue queue, Worker worker, Duration timeoutIfNoWork) {
        this.queue = queue;
        this.worker = worker;
        this.timeoutIfNoWork = timeoutIfNoWork;
    }

    public void run() throws InterruptedException {
        while (true) {
            Optional<AccountId> optionalAccountId = queue.takeNextAvailable();

            if (optionalAccountId.isPresent()) {
                processAccountOperations(optionalAccountId.get());
                Thread.sleep(0); // allow interruption
            } else {
                Thread.sleep(timeoutIfNoWork.toMillis()); // allow interruption
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
        } catch (Exception e) {
            logger.error(format("Failed to evaluate operation for Account '%s'", accountId), e);
            queue.add(accountId);
        } finally {
            workInProgress = Optional.empty();
        }
    }
}
