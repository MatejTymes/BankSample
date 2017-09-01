package mtymes.account.dao.mongo;

import javafixes.concurrency.Runner;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.test.ThreadSynchronizer;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.LongStream.rangeClosed;
import static javafixes.common.CollectionUtil.newList;
import static javafixes.concurrency.Runner.runner;
import static mtymes.account.dao.mongo.Collections.operationsCollection;
import static mtymes.account.domain.account.Version.version;
import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;
import static mtymes.account.domain.operation.OpLogId.opLogId;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
                new DepositTo(randomAccountId(), randomPositiveDecimal()),
                new WithdrawFrom(randomAccountId(), randomPositiveDecimal()),
                new TransferFrom(new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveDecimal())),
                new TransferTo(new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveDecimal()))
        );
        for (Operation operation : allOperations) {
            // When
            OpLogId opLogId = operationDao.storeOperation(operation);

            // Then
            Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(opLogId);
            assertThat(actualOperation, isPresentAndEqualTo(newOperation(opLogId, operation)));
        }
    }

    @Test
    public void shouldStoreOperationsWithSequentialOpLogIdForEachAccount() {
        AccountId accountId1 = randomAccountId();
        AccountId accountId2 = randomAccountId();
        AccountId accountId3 = randomAccountId();
        assertThat(operationDao.storeOperation(randomOperation(accountId1)), equalTo(opLogId(accountId1, version(1))));
        assertThat(operationDao.storeOperation(randomOperation(accountId1)), equalTo(opLogId(accountId1, version(2))));
        assertThat(operationDao.storeOperation(randomOperation(accountId2)), equalTo(opLogId(accountId2, version(1))));
        assertThat(operationDao.storeOperation(randomOperation(accountId1)), equalTo(opLogId(accountId1, version(3))));
        assertThat(operationDao.storeOperation(randomOperation(accountId1)), equalTo(opLogId(accountId1, version(4))));
        assertThat(operationDao.storeOperation(randomOperation(accountId2)), equalTo(opLogId(accountId2, version(2))));
        assertThat(operationDao.storeOperation(randomOperation(accountId3)), equalTo(opLogId(accountId3, version(1))));
        assertThat(operationDao.storeOperation(randomOperation(accountId2)), equalTo(opLogId(accountId2, version(3))));
    }

    @Test
    public void shouldMarkOperationAsSuccessful() {
        Operation operation = randomOperation();
        OpLogId opLogId = operationDao.storeOperation(operation);

        // When
        boolean success = operationDao.markAsSuccessful(opLogId);

        // Then
        assertThat(success, is(true));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(opLogId);
        assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(opLogId, operation)));
    }

    @Test
    public void shouldMarkOperationAsFailed() {
        Operation operation = randomOperation();
        OpLogId opLogId = operationDao.storeOperation(operation);

        // When
        boolean success = operationDao.markAsFailed(opLogId, "failure description");

        // Then
        assertThat(success, is(true));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(opLogId);
        assertThat(actualOperation, isPresentAndEqualTo(failedOperation(opLogId, operation, "failure description")));
    }

    @Test
    public void shouldNotMarkOperationAsSuccessfulTwice() {
        Operation operation = randomOperation();
        OpLogId opLogId = operationDao.storeOperation(operation);
        operationDao.markAsSuccessful(opLogId);

        // When
        boolean success = operationDao.markAsSuccessful(opLogId);

        // Then
        assertThat(success, is(false));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(opLogId);
        assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(opLogId, operation)));
    }

    @Test
    public void shouldNotMarkOperationAsFailedTwice() {
        Operation operation = randomOperation();
        OpLogId opLogId = operationDao.storeOperation(operation);
        operationDao.markAsFailed(opLogId, "first commentary");

        // When
        boolean success = operationDao.markAsFailed(opLogId, "second commentary");

        // Then
        assertThat(success, is(false));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(opLogId);
        assertThat(actualOperation, isPresentAndEqualTo(failedOperation(opLogId, operation, "first commentary")));
    }

    @Test
    public void shouldNotMarkOperationAsSuccessfulIfItIsAlreadyFailed() {
        Operation operation = randomOperation();
        OpLogId opLogId = operationDao.storeOperation(operation);
        operationDao.markAsFailed(opLogId, "first commentary");

        // When
        boolean success = operationDao.markAsSuccessful(opLogId);

        // Then
        assertThat(success, is(false));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(opLogId);
        assertThat(actualOperation, isPresentAndEqualTo(failedOperation(opLogId, operation, "first commentary")));
    }

    @Test
    public void shouldNotMarkOperationAsFailedIfItIsAlreadySuccessful() {
        Operation operation = randomOperation();
        OpLogId opLogId = operationDao.storeOperation(operation);
        operationDao.markAsSuccessful(opLogId);

        // When
        boolean success = operationDao.markAsFailed(opLogId, "failure description");

        // Then
        assertThat(success, is(false));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(opLogId);
        assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(opLogId, operation)));
    }

    @Test
    public void shouldFindUnfinishedOperationLogIds() {
        AccountId accountId = randomAccountId();
        AccountId otherAccountId = randomAccountId();

        OpLogId opLogId1 = operationDao.storeOperation(randomOperation(accountId));
        OpLogId otherOpLogId1 = operationDao.storeOperation(randomOperation(otherAccountId));
        OpLogId opLogId2 = operationDao.storeOperation(randomOperation(accountId));
        OpLogId opLogId3 = operationDao.storeOperation(randomOperation(accountId));
        OpLogId otherOpLogId2 = operationDao.storeOperation(randomOperation(otherAccountId));
        OpLogId opLogId4 = operationDao.storeOperation(randomOperation(accountId));
        OpLogId otherOpLogId3 = operationDao.storeOperation(randomOperation(otherAccountId));
        OpLogId opLogId5 = operationDao.storeOperation(randomOperation(accountId));

        operationDao.markAsSuccessful(opLogId3);
        operationDao.markAsFailed(opLogId4, "Failed");
        operationDao.markAsSuccessful(otherOpLogId2);
        operationDao.markAsFailed(otherOpLogId3, "Failed");

        // When
        List<OpLogId> unFinishedOpLogIds = operationDao.findUnfinishedOperationLogIds(accountId);

        // Then
        assertThat(unFinishedOpLogIds, equalTo(newList(opLogId1, opLogId2, opLogId5)));
    }

    @Test
    public void shouldCreateUniqueSequentialOpLogIdsOnConcurrentWrites() {
        int threadCount = 64;

        List<OpLogId> opLogIds = newCopyOnWriteArrayList();

        List<AccountId> accountIds = rangeClosed(1, 10).mapToObj(value -> randomAccountId()).collect(toList());
        Map<AccountId, AtomicInteger> highestId = accountIds.stream().collect(toMap(
                accountId -> accountId,
                accountId -> new AtomicInteger(0)
        ));

        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                AccountId accountId = pickRandomValue(accountIds);
                Operation operation = randomOperation(accountId);

                synchronizer.blockUntilAllThreadsCallThisMethod();

                // When
                opLogIds.add(
                        operationDao.storeOperation(operation)
                );

                highestId.get(accountId).incrementAndGet();
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        assertThat(opLogIds.size(), is(threadCount));

        // todo: implement with opLogIds
//        Set<SeqId> expectedSeqIds = rangeClosed(1, threadCount)
//                .mapToObj(SeqId::seqId)
//                .collect(toSet());
//        assertThat(newSet(opLogIds), equalTo(expectedSeqIds));
    }

    @Test
    public void shouldAllowOnlyOneFinalizationMethodOnConcurrentRequests() {
        int threadCount = 64;

        Operation operation = randomOperation();
        OpLogId opLogId = operationDao.storeOperation(operation);

        List<FinalState> appliedStates = newCopyOnWriteArrayList();

        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                FinalState stateToApply = pickRandomValue(FinalState.values());

                synchronizer.blockUntilAllThreadsCallThisMethod();

                // When
                boolean success;
                if (stateToApply == FinalState.Success) {
                    success = operationDao.markAsSuccessful(opLogId);
                } else {
                    success = operationDao.markAsFailed(opLogId, "some description");
                }

                if (success) {
                    appliedStates.add(stateToApply);
                }
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        assertThat(appliedStates.size(), is(1));

        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(opLogId);
        if (appliedStates.get(0) == FinalState.Success) {
            assertThat(actualOperation, isPresentAndEqualTo(successfulOperation(opLogId, operation)));
        } else {
            assertThat(actualOperation, isPresentAndEqualTo(failedOperation(opLogId, operation, "some description")));
        }
    }

    private LoggedOperation newOperation(OpLogId opLogId, Operation operation) {
        return new LoggedOperation(opLogId, operation, Optional.empty(), Optional.empty());
    }

    private LoggedOperation successfulOperation(OpLogId opLogId, Operation operation) {
        return new LoggedOperation(opLogId, operation, Optional.of(Success), Optional.empty());
    }

    private LoggedOperation failedOperation(OpLogId opLogId, Operation operation, String description) {
        return new LoggedOperation(opLogId, operation, Optional.of(Failure), Optional.of(description));
    }
}