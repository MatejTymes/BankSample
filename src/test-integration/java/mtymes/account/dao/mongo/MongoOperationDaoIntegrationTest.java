package mtymes.account.dao.mongo;

import mtymes.account.dao.OperationDao;
import mtymes.account.domain.operation.*;
import mtymes.account.exception.DuplicateItemException;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static javafixes.common.CollectionUtil.newList;
import static mtymes.account.dao.mongo.MongoCollections.operationsCollection;
import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.test.ConcurrencyUtil.runConcurrentlyOnNThreads;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
                new CreateAccount(randomOperationId(), randomAccountId()),
                new DepositTo(randomOperationId(), randomAccountId(), randomPositiveAmount()),
                new WithdrawFrom(randomOperationId(), randomAccountId(), randomPositiveAmount()),
                new TransferFrom(randomOperationId(), randomOperationId(), new TransferDetail(randomAccountId(), randomAccountId(), randomPositiveAmount())),
                new TransferTo(randomOperationId(), new TransferDetail(randomAccountId(), randomAccountId(), randomPositiveAmount()))
        );
        for (Operation operation : allOperations) {
            // When
            operationDao.storeOperation(operation);

            // Then
            Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(operation.operationId);
            assertThat(actualOperation, isPresentAndEqualTo(newOperation(operation)));
        }
    }

    @Test
    public void shouldFailToStoreTwoOperationsUnderTheSameOperationId() {
        OperationId operationId = randomOperationId();
        operationDao.storeOperation(randomOperation(operationId));

        try {
            // When
            operationDao.storeOperation(randomOperation(operationId));

            // Then
            fail("expected DuplicateItemException");
        } catch (DuplicateItemException expectedException) {
            // expected
        }
    }

    @Test
    public void shouldMarkOperationAsApplied() {
        OperationId operationId = randomOperationId();
        Operation operation = randomOperation(operationId);
        operationDao.storeOperation(operation);

        // When
        boolean success = operationDao.markAsApplied(operationId);

        // Then
        assertThat(success, is(true));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(appliedOperation(operation)));
    }

    @Test
    public void shouldMarkOperationAsRejected() {
        OperationId operationId = randomOperationId();
        Operation operation = randomOperation(operationId);
        operationDao.storeOperation(operation);

        // When
        boolean success = operationDao.markAsRejected(operationId, "failure description");

        // Then
        assertThat(success, is(true));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(rejectedOperation(operation, "failure description")));
    }

    @Test
    public void shouldNotMarkOperationAsAppliedTwice() {
        OperationId operationId = randomOperationId();
        Operation operation = randomOperation(operationId);
        operationDao.storeOperation(operation);
        operationDao.markAsApplied(operationId);

        // When
        boolean success = operationDao.markAsApplied(operationId);

        // Then
        assertThat(success, is(false));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(appliedOperation(operation)));
    }

    @Test
    public void shouldNotMarkOperationAsRejectedTwice() {
        OperationId operationId = randomOperationId();
        Operation operation = randomOperation(operationId);
        operationDao.storeOperation(operation);
        operationDao.markAsRejected(operationId, "first commentary");

        // When
        boolean success = operationDao.markAsRejected(operationId, "second commentary");

        // Then
        assertThat(success, is(false));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(rejectedOperation(operation, "first commentary")));
    }

    @Test
    public void shouldNotMarkOperationAsAppliedIfItIsAlreadyRejected() {
        OperationId operationId = randomOperationId();
        Operation operation = randomOperation(operationId);
        operationDao.storeOperation(operation);
        operationDao.markAsRejected(operationId, "first commentary");

        // When
        boolean success = operationDao.markAsApplied(operationId);

        // Then
        assertThat(success, is(false));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(rejectedOperation(operation, "first commentary")));
    }

    @Test
    public void shouldNotMarkOperationAsRejectedIfItIsAlreadyApplied() {
        OperationId operationId = randomOperationId();
        Operation operation = randomOperation(operationId);
        operationDao.storeOperation(operation);
        operationDao.markAsApplied(operationId);

        // When
        boolean success = operationDao.markAsRejected(operationId, "failure description");

        // Then
        assertThat(success, is(false));
        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(operationId);
        assertThat(actualOperation, isPresentAndEqualTo(appliedOperation(operation)));
    }

    @Test
    public void shouldAllowOnlyOneFinalizationMethodOnConcurrentRequests() {
        int threadCount = 64;

        OperationId operationId = randomOperationId();
        Operation operation = randomOperation(operationId);
        operationDao.storeOperation(operation);

        List<FinalState> appliedStates = newCopyOnWriteArrayList();

        runConcurrentlyOnNThreads(
                () -> {
                    FinalState stateToApply = pickRandomValue(FinalState.values());

                    // When
                    boolean success;
                    if (stateToApply == FinalState.Applied) {
                        success = operationDao.markAsApplied(operationId);
                    } else {
                        success = operationDao.markAsRejected(operationId, "some description");
                    }

                    if (success) {
                        appliedStates.add(stateToApply);
                    }
                },
                threadCount
        );

        // Then
        assertThat(appliedStates.size(), is(1));

        Optional<LoggedOperation> actualOperation = operationDao.findLoggedOperation(operationId);
        if (appliedStates.get(0) == FinalState.Applied) {
            assertThat(actualOperation, isPresentAndEqualTo(appliedOperation(operation)));
        } else {
            assertThat(actualOperation, isPresentAndEqualTo(rejectedOperation(operation, "some description")));
        }
    }

    private LoggedOperation newOperation(Operation operation) {
        return new LoggedOperation(operation, Optional.empty(), Optional.empty());
    }

    private LoggedOperation appliedOperation(Operation operation) {
        return new LoggedOperation(operation, Optional.of(Applied), Optional.empty());
    }

    private LoggedOperation rejectedOperation(Operation operation, String description) {
        return new LoggedOperation(operation, Optional.of(Rejected), Optional.of(description));
    }
}