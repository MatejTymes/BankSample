package mtymes.account.dao;

import javafixes.concurrency.Runner;
import mtymes.account.domain.operation.*;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.hamcrest.Matchers;
import org.junit.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static javafixes.common.CollectionUtil.newList;
import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.domain.operation.PersistedOperation.newOperation;
import static mtymes.account.mongo.Collections.operationsCollection;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.randomAccountId;
import static mtymes.test.Random.randomPositiveDecimal;
import static org.junit.Assert.assertThat;

public class OperationDaoIntegrationTest {

    private final List<Operation> allOperations = newList(
            new CreateAccount(randomAccountId()),
            new DepositMoney(randomAccountId(), randomPositiveDecimal()),
            new WithdrawMoney(randomAccountId(), randomPositiveDecimal()),
            new InternalTransfer(randomAccountId(), randomAccountId(), randomPositiveDecimal())
    );

    private static EmbeddedDB db;
    private static OperationDao operationDao;

    @BeforeClass
    public static void initDB() {
        db = MongoManager.getEmbeddedDB();
        operationDao = new OperationDao(operationsCollection(db.getDatabase()));
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
    public void shouldBeAbleToStoreAndLoadEachOperation() {
        for (Operation operation : allOperations) {
            // When
            OperationId operationId = operationDao.storeOperation(operation);

            // Then
            Optional<PersistedOperation> actualOperation = operationDao.findOperation(operationId);
            assertThat(actualOperation, isPresentAndEqualTo(newOperation(operationId, operation)));
        }
    }


    // todo: remove - put into performance test instead
    @Test
    @Ignore
    public void shouldBeAbleToStoreOperationsConcurrently() throws InterruptedException {
        int threadCount = 128;
        int insertCount = 100_000;
        AtomicInteger insertCounter = new AtomicInteger(insertCount);

        Runner runner = Runner.runner(threadCount);
        CountDownLatch startAtTheSameTimeBarrier = new CountDownLatch(threadCount + 1);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startAtTheSameTimeBarrier.countDown();
                startAtTheSameTimeBarrier.await();

                while (insertCounter.getAndDecrement() > 0) {
                    operationDao.storeOperation(new CreateAccount(newAccountId()));
                }
            });
        }

        startAtTheSameTimeBarrier.countDown();
        startAtTheSameTimeBarrier.await();

        long startTime = System.currentTimeMillis();
        runner.waitTillDone();
        long endTime = System.currentTimeMillis();

        runner.shutdown();

        double insertsPerSecond = (insertCount * 1000d) / (endTime - startTime);
        System.out.println(insertsPerSecond + " inserts/second");
        assertThat(insertsPerSecond, Matchers.greaterThanOrEqualTo(1_000d));
    }


}