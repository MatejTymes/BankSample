package mtymes.account;

import mtymes.account.domain.account.AccountId;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

import static com.google.common.collect.Sets.newLinkedHashSet;

// todo: rename to SetQueue<T> and move into common
public class ToProcessQueue {

    private final StampedLock lock = new StampedLock();
    private final Set<AccountId> accountIds = newLinkedHashSet();

    public void add(AccountId accountId) {
        long stamp = lock.writeLock();
        try {
            accountIds.add(accountId);
        } finally {
            lock.unlock(stamp);
        }
    }

    public Optional<AccountId> takeNextAvailable() {
        long stamp = lock.writeLock();
        try {
            Iterator<AccountId> iterator = accountIds.iterator();
            if (iterator.hasNext()) {
                AccountId accountId = iterator.next();
                iterator.remove();
                return Optional.of(accountId);
            } else {
                return Optional.empty();
            }
        } finally {
            lock.unlock(stamp);
        }
    }

    // todo: test this
    public int size() {
        long stamp = lock.writeLock();
        try {
            return accountIds.size();
        } finally {
            lock.unlock(stamp);
        }
    }
}
