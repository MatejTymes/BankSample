package mtymes.account;

import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.handler.HandlerDispatcher;
import mtymes.account.handler.OperationHandler;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

// todo: test
public class Worker {

    private Logger logger = getLogger(Worker.class);

    private final WorkQueue queue;
    private final OperationDao operationDao;
    private final HandlerDispatcher dispatcher;
    private final Duration timeoutIfNoWork;

    public Worker(WorkQueue queue, OperationDao operationDao, HandlerDispatcher dispatcher, Duration timeoutIfNoWork) {
        this.queue = queue;
        this.operationDao = operationDao;
        this.dispatcher = dispatcher;
        this.timeoutIfNoWork = timeoutIfNoWork;
    }

    public void run() throws InterruptedException {
        while (true) {
            Optional<AccountId> optionalAccountId = queue.takeNextAvailable();

            if (!optionalAccountId.isPresent()) {
                processAccountOperations(optionalAccountId.get());
                Thread.sleep(0); // allow interruption
            } else {
                Thread.sleep(timeoutIfNoWork.toMillis());
            }
        }
    }

    private void processAccountOperations(AccountId accountId) {
        try {
            List<OpLogId> unfinishedOpLogIds = operationDao.findUnfinishedOperationLogIds(accountId);
            for (OpLogId unfinishedOpLogId : unfinishedOpLogIds) {
                LoggedOperation loggedOperation = operationDao.findLoggedOperation(unfinishedOpLogId).get();
                if (!loggedOperation.isFinished()) {
                    OperationHandler handler = loggedOperation.operation.apply(dispatcher);
                    handler.handleOperation(loggedOperation.opLogId, loggedOperation.operation);
                }
            }
        } catch (Exception e) {
            logger.error(format("Failed to evaluate operation for Account '%s'", accountId), e);
            queue.add(accountId);
        }
    }
}
