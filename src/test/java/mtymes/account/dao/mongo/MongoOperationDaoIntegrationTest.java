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
import static mtymes.account.domain.operation.PersistedOperation.*;
import static mtymes.account.domain.operation.SeqId.seqId;
import static mtymes.account.mongo.Collections.operationsCollection;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

// todo: move into test-integration
public class MongoOperationDaoIntegrationTest {

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
        List<Operation> allOperations = newList(
                new CreateAccount(randomAccountId()),
                new DepositMoney(randomAccountId(), randomPositiveDecimal()),
                new WithdrawMoney(randomAccountId(), randomPositiveDecimal()),
                new TransferMoneyFrom(new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveDecimal())),
                new TransferMoneyTo(new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveDecimal()))
        );
        for (Operation operation : allOperations) {
            // When
            SeqId seqId = operationDao.storeOperation(operation);

            // Then
            Optional<PersistedOperation> actualOperation = operationDao.findOperation(seqId);
            assertThat(actualOperation, isPresentAndEqualTo(newOperation(seqId, operation)));
        }
    }

    @Test
    public void shouldStoreOperationsWithSequentialSeqId() {
        assertThat(operationDao.storeOperation(randomOperation()), equalTo(seqId(1)));
        assertThat(operationDao.storeOperation(randomOperation()), equalTo(seqId(2)));
        assertThat(operationDao.storeOperation(randomOperation()), equalTo(seqId(3)));
        assertThat(operationDao.storeOperation(randomOperation()), equalTo(seqId(4)));
        assertThat(operationDao.storeOperation(randomOperation()), equalTo(seqId(5)));
    }

    @Test
    public void shouldMarkOperationAsSuccessful() {
        Operation operation = randomOperation();
        SeqId seqId = operationDao.storeOperation(operation);

        // When
        boolean success = operationDao.markAsSuccessful(seqId);

        // Then
        assertThat(success, is(true));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(seqId);
        assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(seqId, operation)));
    }

    @Test
    public void shouldMarkOperationAsFailed() {
        Operation operation = randomOperation();
        SeqId seqId = operationDao.storeOperation(operation);

        // When
        boolean success = operationDao.markAsFailed(seqId, "failure description");

        // Then
        assertThat(success, is(true));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(seqId);
        assertThat(actualOperation, isPresentAndEqualTo(failedOperation(seqId, operation, "failure description")));
    }

    @Test
    public void shouldNotMarkOperationAsSuccessfulTwice() {
        Operation operation = randomOperation();
        SeqId seqId = operationDao.storeOperation(operation);
        operationDao.markAsSuccessful(seqId);

        // When
        boolean success = operationDao.markAsSuccessful(seqId);

        // Then
        assertThat(success, is(false));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(seqId);
        assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(seqId, operation)));
    }

    @Test
    public void shouldNotMarkOperationAsFailedTwice() {
        Operation operation = randomOperation();
        SeqId seqId = operationDao.storeOperation(operation);
        operationDao.markAsFailed(seqId, "first commentary");

        // When
        boolean success = operationDao.markAsFailed(seqId, "second commentary");

        // Then
        assertThat(success, is(false));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(seqId);
        assertThat(actualOperation, isPresentAndEqualTo(failedOperation(seqId, operation, "first commentary")));
    }

    @Test
    public void shouldNotMarkOperationAsSuccessfulIfItIsAlreadyFailed() {
        Operation operation = randomOperation();
        SeqId seqId = operationDao.storeOperation(operation);
        operationDao.markAsFailed(seqId, "first commentary");

        // When
        boolean success = operationDao.markAsSuccessful(seqId);

        // Then
        assertThat(success, is(false));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(seqId);
        assertThat(actualOperation, isPresentAndEqualTo(failedOperation(seqId, operation, "first commentary")));
    }

    @Test
    public void shouldNotMarkOperationAsFailedIfItIsAlreadySuccessful() {
        Operation operation = randomOperation();
        SeqId seqId = operationDao.storeOperation(operation);
        operationDao.markAsSuccessful(seqId);

        // When
        boolean success = operationDao.markAsFailed(seqId, "failure description");

        // Then
        assertThat(success, is(false));
        Optional<PersistedOperation> actualOperation = operationDao.findOperation(seqId);
        assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(seqId, operation)));
    }

    @Test
    public void shouldCreateUniqueSeqIdsOnConcurrentWrites() {
        int threadCount = 64;

        List<SeqId> seqIds = newCopyOnWriteArrayList();

        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                Operation operation = randomOperation();

                synchronizer.synchronizeThreadsAtThisPoint();

                // When
                seqIds.add(
                        operationDao.storeOperation(operation)
                );
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        assertThat(seqIds.size(), is(threadCount));
        Set<SeqId> expectedSeqIds = rangeClosed(1, threadCount)
                .mapToObj(SeqId::seqId)
                .collect(toSet());
        assertThat(newSet(seqIds), equalTo(expectedSeqIds));
    }

    @Test
    public void shouldAllowOnlyOneFinalizationMethodOnConcurrentRequests() {
        int threadCount = 64;

        Operation operation = randomOperation();
        SeqId seqId = operationDao.storeOperation(operation);

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
                    success = operationDao.markAsSuccessful(seqId);
                } else {
                    success = operationDao.markAsFailed(seqId, "some description");
                }

                if (success) {
                    appliedStates.add(stateToApply);
                }
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        assertThat(appliedStates.size(), is(1));

        Optional<PersistedOperation> actualOperation = operationDao.findOperation(seqId);
        if (appliedStates.get(0) == FinalState.Success) {
            assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(seqId, operation)));
        } else {
            assertThat(actualOperation, isPresentAndEqualTo(failedOperation(seqId, operation, "some description")));
        }
    }
}