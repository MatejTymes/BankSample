package mtymes.account.handler;

import mtymes.account.domain.account.AccountId;

import java.util.Optional;
import java.util.Queue;

import static com.google.common.collect.Queues.newConcurrentLinkedQueue;

// todo: test
public class ToProcessQueue {

    // todo: maybe use Set and block (to prevent duplicates)
    private final Queue<AccountId> accountIds = newConcurrentLinkedQueue();

    public void add(AccountId accountId) {
        accountIds.add(accountId);
    }

    public Optional<AccountId> takeNextAvailable() {
        return Optional.ofNullable(accountIds.poll());
    }
}
