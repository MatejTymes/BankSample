package mtymes.account.dao.mongo;

import javafixes.object.Tuple;
import mtymes.account.dao.OpLogDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.exception.DuplicateItemException;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static java.util.stream.Collectors.*;
import static java.util.stream.IntStream.rangeClosed;
import static javafixes.common.CollectionUtil.newList;
import static javafixes.common.CollectionUtil.newSet;
import static javafixes.object.Tuple.tuple;
import static mtymes.account.dao.mongo.MongoCollections.opLogCollection;
import static mtymes.account.domain.operation.SeqId.seqId;
import static mtymes.test.ConcurrencyUtil.runConcurrentlyOnNThreads;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MongoOpLogDaoIntegrationTest {

    private static EmbeddedDB db;
    private static OpLogDao opLogDao;

    @BeforeClass
    public static void initDB() {
        db = MongoManager.getEmbeddedDB();
        opLogDao = new MongoOpLogDao(opLogCollection(db.getDatabase()));
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
    public void shouldStoreOperationsWithSequentialOpLogIdForEachAccount() {
        AccountId accountId1 = randomAccountId();
        AccountId accountId2 = randomAccountId();
        AccountId accountId3 = randomAccountId();
        assertThat(opLogDao.registerOperationId(accountId1, randomOperationId()), equalTo(seqId(1)));
        assertThat(opLogDao.registerOperationId(accountId1, randomOperationId()), equalTo(seqId(2)));
        assertThat(opLogDao.registerOperationId(accountId2, randomOperationId()), equalTo(seqId(1)));
        assertThat(opLogDao.registerOperationId(accountId1, randomOperationId()), equalTo(seqId(3)));
        assertThat(opLogDao.registerOperationId(accountId1, randomOperationId()), equalTo(seqId(4)));
        assertThat(opLogDao.registerOperationId(accountId2, randomOperationId()), equalTo(seqId(2)));
        assertThat(opLogDao.registerOperationId(accountId3, randomOperationId()), equalTo(seqId(1)));
        assertThat(opLogDao.registerOperationId(accountId2, randomOperationId()), equalTo(seqId(3)));
    }

    @Test
    public void shouldFailToRegisterTheSameOperationIdTwice() {
        AccountId accountId = randomAccountId();
        OperationId operationId = randomOperationId();

        opLogDao.registerOperationId(accountId, operationId);

        try {
            // When
            opLogDao.registerOperationId(accountId, operationId);

            // Then
            fail("expected DuplicateItemException");
        } catch (DuplicateItemException expectedException) {
            // expected
        }
    }

    @Test
    public void shouldFindUnfinishedOperationLogIds() {
        AccountId accountId = randomAccountId();
        AccountId otherAccountId = randomAccountId();

        OperationId operationId1 = randomOperationId();
        OperationId operationId2 = randomOperationId();
        OperationId operationId3 = randomOperationId();
        OperationId otherOperationId1 = randomOperationId();
        OperationId otherOperationId2 = randomOperationId();

        SeqId seqId1 = opLogDao.registerOperationId(accountId, operationId1);
        opLogDao.registerOperationId(accountId, operationId2);
        SeqId seqId3 = opLogDao.registerOperationId(accountId, operationId3);
        opLogDao.registerOperationId(otherAccountId, otherOperationId1);
        opLogDao.registerOperationId(otherAccountId, otherOperationId2);

        opLogDao.markAsFinished(operationId2);
        opLogDao.markAsFinished(otherOperationId1);

        // When
        List<Tuple<OperationId, SeqId>> unFinishedOpLogIds = opLogDao.findUnfinishedOperationIds(accountId);

        // Then
        assertThat(unFinishedOpLogIds, equalTo(newList(
                tuple(operationId1, seqId1),
                tuple(operationId3, seqId3)
        )));
    }

    @Test
    public void shouldCreateUniqueSequentialOpLogIdsOnConcurrentWrites() {
        int threadCount = 64;

        List<Tuple<AccountId, SeqId>> seqIds = newCopyOnWriteArrayList();

        List<AccountId> accountIds = rangeClosed(1, 10).mapToObj(value -> randomAccountId()).collect(toList());
        Map<AccountId, AtomicInteger> highestId = accountIds.stream().collect(toMap(
                accountId -> accountId,
                accountId -> new AtomicInteger(0)
        ));

        runConcurrentlyOnNThreads(
                () -> {
                    AccountId accountId = pickRandomValue(accountIds);
                    OperationId operationId = randomOperationId();

                    // When
                    seqIds.add(tuple(
                            accountId,
                            opLogDao.registerOperationId(accountId, operationId)
                    ));

                    highestId.get(accountId).incrementAndGet();
                },
                threadCount
        );

        // Then
        assertThat(seqIds.size(), is(threadCount));

        Set<Tuple<AccountId, SeqId>> expectedSeqIds = highestId.entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0)
                .flatMap(entry -> rangeClosed(1, entry.getValue().get()).mapToObj(seqId -> tuple(entry.getKey(), seqId(seqId))))
                .collect(toSet());
        assertThat(newSet(seqIds), equalTo(expectedSeqIds));
    }
}