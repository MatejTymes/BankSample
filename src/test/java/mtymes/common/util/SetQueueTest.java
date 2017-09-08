package mtymes.common.util;

import mtymes.account.domain.account.AccountId;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static javafixes.common.CollectionUtil.newList;
import static javafixes.common.CollectionUtil.newSet;
import static mtymes.test.ConcurrencyUtil.runConcurrentlyOnNThreads;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.randomAccountId;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class SetQueueTest {

    private SetQueue<AccountId> queue = new SetQueue<>();

    @Test
    public void shouldBeAbleToRetrieveItemsInOrder() {
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
    public void shouldIgnoreDuplicateItems() {
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
    public void shouldBeAbleToInsertAndRetrieveItemsConcurrently() {
        int threadCount = 50;

        List<AccountId> accountIds = newList(
                randomAccountId(),
                randomAccountId(),
                randomAccountId(),
                randomAccountId(),
                randomAccountId()
        );

        // When
        runConcurrentlyOnNThreads(
                index -> queue.add(accountIds.get(index % accountIds.size())),
                threadCount
        );

        List<AccountId> retrievedAccountIds = newCopyOnWriteArrayList();
        runConcurrentlyOnNThreads(
                () -> queue.takeNextAvailable().ifPresent(retrievedAccountIds::add),
                threadCount
        );

        // Then
        assertThat(retrievedAccountIds.size(), equalTo(accountIds.size()));
        assertThat(newSet(retrievedAccountIds).size(), equalTo(accountIds.size()));
        assertThat(newSet(retrievedAccountIds), equalTo(newSet(accountIds)));
    }
}