package mtymes.account.dao.mongo;

import javafixes.concurrency.Runner;
import mtymes.account.dao.OperationDao;
import mtymes.test.ThreadSynchronizer;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static javafixes.concurrency.Runner.runner;
import static mtymes.account.dao.mongo.Collections.operationsCollection;
import static mtymes.test.Random.randomOperation;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

public class MongoOperationDaoPerformanceTest {

    private static EmbeddedDB db;
    private static OperationDao operationDao;

    @BeforeClass
    public static void initDB() {
        db = MongoManager.getEmbeddedDB();
        operationDao = new MongoOperationDao(operationsCollection(db.getDatabase()));
    }

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
    }

    @AfterClass
    public static void releaseDB() {
        MongoManager.release(db);
    }

    @Test
    public void shouldStoreAtLeast1000OpPerSecondOnConcurrentWrites() throws InterruptedException {
        int threadCount = 128;
        int insertCount = 25_000;
        AtomicInteger insertCounter = new AtomicInteger(insertCount);

        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount + 1);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                synchronizer.synchronizeThreadsAtThisPoint();

                while (insertCounter.getAndDecrement() > 0) {
                    operationDao.storeOperation(randomOperation());
                }
            });
        }

        synchronizer.synchronizeThreadsAtThisPoint();

        long startTime = System.currentTimeMillis();
        runner.waitTillDone();
        long endTime = System.currentTimeMillis();

        runner.shutdown();

        double insertsPerSecond = (insertCount * 1000d) / (endTime - startTime);
        System.out.println(insertsPerSecond + " inserts/second");
        assertThat(insertsPerSecond, greaterThanOrEqualTo(1_000d));
    }
}
