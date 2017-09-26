package mtymes.app;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.app.Bank;
import mtymes.account.config.SystemProperties;
import mtymes.account.domain.QueuedWorkStats;
import mtymes.account.domain.account.AccountId;
import mtymes.api.BankApi;
import mtymes.api.ResponseWrapper;
import mtymes.test.ThreadSynchronizer;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static de.flapdoodle.embed.process.runtime.Network.getFreeServerPort;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static javafixes.common.CollectionUtil.newList;
import static javafixes.concurrency.Runner.runner;
import static javafixes.math.Decimal.ZERO;
import static javafixes.math.Decimal.d;
import static mtymes.common.json.JsonBuilder.jsonBuilder;
import static mtymes.test.ConcurrencyUtil.runConcurrentlyOnNThreads;
import static mtymes.test.Random.pickRandomValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class ConcurrencySystemTest {

    private static EmbeddedDB db;
    private static Bank appNode;
    private static BankApi api;

    @BeforeClass
    public static void initDbAndApp() throws IOException {
        db = MongoManager.getEmbeddedDB();

        SystemProperties properties = new SystemProperties(
                getFreeServerPort(),
                "localhost",
                db.getPort(),
                db.getDbName(),
                10,
                Duration.ofMillis(0)
        );
        appNode = new Bank(properties).start();

        api = new BankApi("localhost", appNode.getPort());
    }

    @AfterClass
    public static void releaseDB() {
        MongoManager.release(db);
        appNode.shutdown();
    }

    @Test
    public void shouldNotTransferAboveExistingAmountOnAccount() {
        Decimal initialDeposit = d("20_000");
        int countOfConcurrentTransfers = 50;
        Decimal transferAmount = d("1_000");

        AccountId fromAccountId = api.createAccount().accountId();
        api.depositMoney(fromAccountId, initialDeposit);

        List<AccountId> toAccountIds = newList();
        for (int i = 0; i < 30; i++) {
            toAccountIds.add(api.createAccount().accountId());
        }

        List<AccountId> successfulTransfers = newCopyOnWriteArrayList();
        // When
        runConcurrentlyOnNThreads(
                () -> {
                    AccountId toAccountId = pickRandomValue(toAccountIds);
                    ResponseWrapper response = api.transferMoney(fromAccountId, toAccountId, transferAmount);
                    if (response.status() == 200) {
                        successfulTransfers.add(toAccountId);
                    }
                },
                countOfConcurrentTransfers
        );
        waitForQueuedWorkToFinish();

        // Then
        assertThat(successfulTransfers.size(), equalTo(initialDeposit.div(transferAmount).intValue()));
        api.loadAccount(fromAccountId).shouldHaveBody(
                jsonBuilder()
                        .with("accountId", fromAccountId)
                        .with("balance", ZERO)
                        .build(),
                false);
        for (AccountId toAccountId : toAccountIds) {
            int transferToCount = (int) successfulTransfers.stream().filter(accountId -> accountId.equals(toAccountId)).count();
            api.loadAccount(toAccountId).shouldHaveBody(jsonBuilder()
                    .with("accountId", toAccountId)
                    .with("balance", transferAmount.multiply(d(transferToCount)))
                    .with("version", 1 + transferToCount)
                    .build());
        }
    }

    @Test
    public void shouldBeAbleToDoAtLeast250TransfersPerSecond() throws InterruptedException {
        int accountCount = 1_000;
        int threadCount = 16;
        int transferCount = 7_500;

        List<AccountId> accountIds = rangeClosed(1, accountCount).mapToObj(i -> {
            AccountId accountId = api.createAccount().accountId();
            api.depositMoney(accountId, d("25_000"));
            return accountId;
        }).collect(toList());

        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount + 1);
        AtomicInteger remainingTransferCounter = new AtomicInteger(transferCount);
        Runner runner = runner(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                while(remainingTransferCounter.getAndDecrement() > 0) {
                    AccountId fromAccountId = pickRandomValue(accountIds);
                    AccountId toAccountId = pickRandomValue(accountIds);
                    Decimal amount = Decimal.TEN;

                    api.transferMoney(fromAccountId, toAccountId, amount);
                }
            });
        }

        synchronizer.blockUntilAllThreadsCallThisMethod();

        long startTime = System.currentTimeMillis();
        runner.waitTillDone();
        long endTime = System.currentTimeMillis();

        runner.shutdown();

        Duration duration = Duration.ofMillis(endTime - startTime);
        double transfersPerSecond = transferCount * 1000d / (endTime - startTime);

        System.out.println("total duration = " + duration);
        System.out.println("op/sec = " + transfersPerSecond);

        assertThat(transfersPerSecond, greaterThan(250d));

        waitForQueuedWorkToFinish(Duration.ofSeconds(30));
    }

    private void waitForQueuedWorkToFinish(Duration duration) {
        long startTime = System.currentTimeMillis();

        boolean retry;
        do {
            long currentTime = System.currentTimeMillis();
            QueuedWorkStats stats = api.queuedWorkStats().bodyAs(QueuedWorkStats.class);
            retry = stats.queuedCount > 0 || stats.inProgressCount > 0;
            if (retry && currentTime - startTime > duration.toMillis()) {
                throw new IllegalStateException("queued work has not finished in " + duration);
            }

            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        } while (retry);
    }

    private void waitForQueuedWorkToFinish() {
        waitForQueuedWorkToFinish(Duration.ofSeconds(2));
    }
}
