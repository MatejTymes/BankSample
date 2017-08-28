package mtymes.playground;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import javafixes.concurrency.Runner;
import mtymes.account.domain.account.AccountId;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.bson.Document;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.mongodb.client.model.Indexes.descending;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static javafixes.common.CollectionUtil.newSet;
import static javafixes.concurrency.Runner.runner;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;
import static mtymes.test.Random.pickRandomValue;
import static mtymes.test.Random.randomAccountId;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

// todo: move into test-playground
@Ignore
public class OpStoragePerformanceTest {

    private static EmbeddedDB db;
    private static MongoCollection<Document> operations;

    @BeforeClass
    public static void initDB() {
        db = MongoManager.getEmbeddedDB();
        operations = operationsCollection(db.getDatabase());
    }

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
    }

    @Test
    public void operationStoragePerformanceTest() throws InterruptedException {
        int threadCount = 128;
        int accountCount = 25_000;
        int operationCount = 150_000;

        AtomicInteger insertCount = new AtomicInteger(operationCount);

        List<AccountId> accountIds = range(0, accountCount).mapToObj(value -> randomAccountId()).collect(toList());

        Runner runner = runner(threadCount);
        CountDownLatch startAtTheSameTime = new CountDownLatch(threadCount + 1);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startAtTheSameTime.countDown();
                startAtTheSameTime.await();

                while (insertCount.getAndDecrement() > 0) {
                    AccountId accountId = pickRandomValue(accountIds);
                    Document document = docBuilder()
                            .put("accountId", accountId)
                            .build();
                    storeWithSequenceId(document, accountId);
                }
            });
        }

        startAtTheSameTime.countDown();
        startAtTheSameTime.await();

        long startTime = System.currentTimeMillis();
        runner.waitTillDone();
        long endTime = System.currentTimeMillis();
        runner.shutdown();

        double opCountPerSecond = (operationCount * 1_000d) / (endTime - startTime);
        System.out.println("Op/second = " + opCountPerSecond);
        assertThat(opCountPerSecond, Matchers.greaterThanOrEqualTo(1_000d));
        assertThat(operations.count(), is((long) operationCount));
    }

    private static void storeWithSequenceId(Document document, AccountId accountId) {
        long idToUse;

        int attemptCount = 0; // use of this is relevant only in case of multi-node scenario

//        long stamp = seqGenerationLock.writeLock();
//        try {
        idToUse = getLastId(accountId) + 1;
        boolean retry;
        do {
            retry = false;
            try {
                document.put("seqId", idToUse);
                operations.insertOne(document);
            } catch (MongoWriteException e) {
                retry = true;
                if (++attemptCount < 3) {
                    idToUse++;
                } else {
                    attemptCount = 0;
                    idToUse = getLastId(accountId) + 1;
                }
            }
        } while (retry);
//        } finally {
//            seqGenerationLock.unlock(stamp);
//        }
    }


    private static long getLastId(AccountId accountId) {
        MongoCursor<Document> idIterator = operations
                .find(doc("accountId", accountId))
                .projection(doc("seqId", 1))
                .sort(doc("seqId", -1)).limit(1)
                .iterator();
        return idIterator.hasNext() ? idIterator.next().getLong("seqId") : 0;
    }

    private static MongoCollection<Document> operationsCollection(MongoDatabase database) {
        return getOrCreateCollection(
                database,
                "operations",
                operations -> operations.createIndex(
                        descending("accountId", "seqId"),
                        new IndexOptions().unique(true)
                )
        );
    }

    private static MongoCollection<Document> getOrCreateCollection(MongoDatabase database, String collectionName, Consumer<MongoCollection<Document>> afterCreation) {
        if (!newSet(database.listCollectionNames()).contains(collectionName)) {
            database.createCollection(collectionName);

            MongoCollection<Document> collection = database.getCollection(collectionName);
            afterCreation.accept(collection);
            return collection;
        } else {
            return database.getCollection(collectionName);
        }
    }
}
