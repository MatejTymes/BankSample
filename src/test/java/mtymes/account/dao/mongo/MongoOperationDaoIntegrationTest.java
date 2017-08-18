package mtymes.account.dao.mongo;

import javafixes.concurrency.Runner;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.operation.*;
import mtymes.test.ThreadSynchronizer;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.LongStream.rangeClosed;
import static javafixes.common.CollectionUtil.newList;
import static javafixes.common.CollectionUtil.newSet;
import static javafixes.concurrency.Runner.runner;
import static mtymes.account.domain.operation.OperationId.operationId;
import static mtymes.account.domain.operation.PersistedOperation.*;
import static mtymes.account.mongo.Collections.operationsCollection;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

// todo: move into test-integration
public class MongoOperationDaoIntegrationTest {

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
    public void shouldBeAbleToStoreAndLoadEachOperation() {
        for (Operation operation : allOperations) {
            // When
            OperationId operationId = operationDao.storeOperation(operation);

            // Then
            Optional<PersistedOperation> actualOperation = operationDao.findOperation(operationId);
            assertThat(actualOperation, isPresentAndEqualTo(newOperation(operationId, operation)));
        }
    }

    @Test
    public void shouldStoreOperationsWithSequentialOperationId() {
        assertThat(operationDao.storeOperation(pickRandomValue(allOperations)), equalTo(operationId(1)));
        assertThat(operationDao.storeOperation(pickRandomValue(allOperations)), equalTo(operationId(2)));
        assertThat(operationDao.storeOperation(pickRandomValue(allOperations)), equalTo(operationId(3)));
        assertThat(operationDao.storeOperation(pickRandomValue(allOperations)), equalTo(operationId(4)));
        assertThat(operationDao.storeOperation(pickRandomValue(allOperations)), equalTo(operationId(5)));
    }

    @Test
    public void shouldMarkOperationAsSuccessful() {
        Operation operation = pickRandomValue(allOperations);
        OperationId operationId = operationDao.storeOperation(operation);

        // When
        boolean success = operationDao.markAsSuccessful(operationId);

        // Then
        assertThat(success, is(true));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(operationId, operation)));
    }

    @Test
    public void shouldMarkOperationAsFailed() {
        Operation operation = pickRandomValue(allOperations);
        OperationId operationId = operationDao.storeOperation(operation);

        // When
        boolean success = operationDao.markAsFailed(operationId, "failure description");

        // Then
        assertThat(success, is(true));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(failedOperation(operationId, operation, "failure description")));
    }

    @Test
    public void shouldNotMarkOperationAsSuccessfulTwice() {
        Operation operation = pickRandomValue(allOperations);
        OperationId operationId = operationDao.storeOperation(operation);
        operationDao.markAsSuccessful(operationId);

        // When
        boolean success = operationDao.markAsSuccessful(operationId);

        // Then
        assertThat(success, is(false));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(operationId, operation)));
    }

    @Test
    public void shouldNotMarkOperationAsFailedTwice() {
        Operation operation = pickRandomValue(allOperations);
        OperationId operationId = operationDao.storeOperation(operation);
        operationDao.markAsFailed(operationId, "first commentary");

        // When
        boolean success = operationDao.markAsFailed(operationId, "second commentary");

        // Then
        assertThat(success, is(false));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(failedOperation(operationId, operation, "first commentary")));
    }

    @Test
    public void shouldNotMarkOperationAsSuccessfulIfItIsAlreadyFailed() {
        Operation operation = pickRandomValue(allOperations);
        OperationId operationId = operationDao.storeOperation(operation);
        operationDao.markAsFailed(operationId, "first commentary");

        // When
        boolean success = operationDao.markAsSuccessful(operationId);

        // Then
        assertThat(success, is(false));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(failedOperation(operationId, operation, "first commentary")));
    }

    @Test
    public void shouldNotMarkOperationAsFailedIfItIsAlreadySuccessful() {
        Operation operation = pickRandomValue(allOperations);
        OperationId operationId = operationDao.storeOperation(operation);
        operationDao.markAsSuccessful(operationId);

        // When
        boolean success = operationDao.markAsFailed(operationId, "failure description");

        // Then
        assertThat(success, is(false));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(operationId, operation)));
    }

    @Test
    public void shouldCreateUniqueSequentialOperationIdsOnConcurrentWrites() {
        int threadCount = 64;

        List<OperationId> operationIds = newCopyOnWriteArrayList();

        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                Operation operation = pickRandomValue(allOperations);

                synchronizer.synchronizeThreadsAtThisPoint();

                // When
                operationIds.add(
                        operationDao.storeOperation(operation)
                );
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        assertThat(operationIds.size(), is(threadCount));
        Set<OperationId> expectedOperationIds = rangeClosed(1, threadCount)
                .mapToObj(OperationId::operationId)
                .collect(toSet());
        assertThat(newSet(operationIds), equalTo(expectedOperationIds));
    }

    @Test
    public void shouldAllowOnlyOneFinalizationMethodOnConcurrentRequests() {
        int threadCount = 64;

        Operation operation = pickRandomValue(allOperations);
        OperationId operationId = operationDao.storeOperation(operation);

        List<FinalState> appliedStates = newCopyOnWriteArrayList();

        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                FinalState stateToApply = pickRandomValue(FinalState.values());

                synchronizer.synchronizeThreadsAtThisPoint();

                // When
                boolean success;
                if (stateToApply == FinalState.Success) {
                    success = operationDao.markAsSuccessful(operationId);
                } else {
                    success = operationDao.markAsFailed(operationId, "some description");
                }

                if (success) {
                    appliedStates.add(stateToApply);
                }
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        assertThat(appliedStates.size(), is(1));

        Optional<PersistedOperation> actualOperation = operationDao.findOperation(operationId);
        if (appliedStates.get(0) == FinalState.Success) {
            assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(operationId, operation)));
        } else {
            assertThat(actualOperation, isPresentAndEqualTo(failedOperation(operationId, operation, "some description")));
        }
    }
}