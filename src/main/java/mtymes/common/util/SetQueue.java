package mtymes.common.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

import static com.google.common.collect.Sets.newLinkedHashSet;

public class SetQueue<T> {

    private final StampedLock lock = new StampedLock();
    private final Set<T> items = newLinkedHashSet();

    public void add(T item) {
        long stamp = lock.writeLock();
        try {
            items.add(item);
        } finally {
            lock.unlock(stamp);
        }
    }

    public Optional<T> takeNextAvailable() {
        long stamp = lock.writeLock();
        try {
            Iterator<T> iterator = items.iterator();
            if (iterator.hasNext()) {
                T item = iterator.next();
                iterator.remove();
                return Optional.of(item);
            } else {
                return Optional.empty();
            }
        } finally {
            lock.unlock(stamp);
        }
    }

    // todo: test this
    public int size() {
        return items.size();
    }
}
