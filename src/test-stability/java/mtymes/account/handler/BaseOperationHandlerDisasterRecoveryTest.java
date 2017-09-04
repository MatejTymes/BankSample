package mtymes.account.handler;

import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.test.BrokenAccountDao;
import mtymes.test.BrokenOperationDao;
import mtymes.test.BrokenSystemException;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class BaseOperationHandlerDisasterRecoveryTest extends BaseOperationHandlerStabilityTest {

    protected AtomicInteger failureCount = new AtomicInteger(0);
    protected AccountDao brokenAccountDao;
    protected OperationDao brokenOperationDao;

    @Before
    public void initDR() {
        brokenAccountDao = new BrokenAccountDao(
                accountDao,
                () -> {
                    failureCount.incrementAndGet();
                    return new BrokenSystemException();
                }
        );
        brokenOperationDao = new BrokenOperationDao(
                operationDao,
                () -> {
                    failureCount.incrementAndGet();
                    return new BrokenSystemException();
                }
        );
    }

    @After
    public void checkFailureOccurred() {
        assertThat("you have not tested any broken db calls, is your code using brokenAccountDao and brokenOperationDao ?", failureCount.get(), greaterThan(0));
    }

    protected void retryWhileSystemIsBroken(Runnable task) {
        boolean systemBroken;
        do {
            systemBroken = false;
            try {
                task.run();
            } catch (BrokenSystemException mightHappen) {
                systemBroken = true;
            }
        } while (systemBroken);
    }
}
