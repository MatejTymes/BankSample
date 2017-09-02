package mtymes.account;

import javafixes.concurrency.Runner;
import mtymes.account.domain.account.AccountId;
import mtymes.test.ThreadSynchronizer;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static javafixes.common.CollectionUtil.newList;
import static javafixes.common.CollectionUtil.newSet;
import static javafixes.concurrency.Runner.runner;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.randomAccountId;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class WorkQueueTest {

    private WorkQueue queue = new WorkQueue();

    @Test
    public void shouldBeAbleToRetrieveAccountIdsInOrder() {
        AccountId accountId1 = randomAccountId();
        AccountId accountId2 = randomAccountId();
        AccountId accountId3 = randomAccountId();
        AccountId accountId4 = randomAccountId();

        // When
        queue.add(accountId1);
        queue.add(accountId2);
        queue.add(accountId3);
        queue.add(accountId4);

        // Then
        assertThat(queue.takeNextAvailable(), isPresentAndEqualTo(accountId1));
        assertThat(queue.takeNextAvailable(), isPresentAndEqualTo(accountId2));
        assertThat(queue.takeNextAvailable(), isPresentAndEqualTo(accountId3));
        assertThat(queue.takeNextAvailable(), isPresentAndEqualTo(accountId4));
        assertThat(queue.takeNextAvailable(), isNotPresent());
    }

    @Test
    public void shouldIgnoreDuplicateAccountIds() {
        AccountId accountId1 = randomAccountId();
        AccountId accountId2 = randomAccountId();
        AccountId accountId3 = randomAccountId();

        // When
        queue.add(accountId1);
        queue.add(accountId1);
        queue.add(accountId2);
        queue.add(accountId1);
        queue.add(accountId2);
        queue.add(accountId3);

        // Then
        assertThat(queue.takeNextAvailable(), isPresentAndEqualTo(accountId1));
        assertThat(queue.takeNextAvailable(), isPresentAndEqualTo(accountId2));
        assertThat(queue.takeNextAvailable(), isPresentAndEqualTo(accountId3));
        assertThat(queue.takeNextAvailable(), isNotPresent());
    }

    @Test
    public void shouldBeAbleToInsertAndRetrieveAccountIdsConcurrently() {
        int threadCount = 50;

        List<AccountId> accountIds = newList(
                randomAccountId(),
                randomAccountId(),
                randomAccountId(),
                randomAccountId(),
                randomAccountId()
        );

        // When
        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final AccountId accountId = accountIds.get(i % accountIds.size());
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                queue.add(accountId);
            });
        }
        runner.waitTillDone();

        List<AccountId> retrievedAccountIds = newCopyOnWriteArrayList();
        ThreadSynchronizer synchronizer2 = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                queue.takeNextAvailable().ifPresent(retrievedAccountIds::add);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        assertThat(retrievedAccountIds.size(), equalTo(accountIds.size()));
        assertThat(newSet(retrievedAccountIds).size(), equalTo(accountIds.size()));
        assertThat(newSet(retrievedAccountIds), equalTo(newSet(accountIds)));
    }
}