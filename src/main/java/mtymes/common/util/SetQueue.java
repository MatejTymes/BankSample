package mtymes.common.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Sets.newLinkedHashSet;

public class SetQueue<T> {

    private final Locker locker = new Locker();
    private final Set<T> items = newLinkedHashSet();

    public void add(T item) {
        locker.lockAndRun((Runnable)
                () -> items.add(item)
        );
    }

    public Optional<T> takeNextAvailable() {
        return locker.lockAndRun(() -> {
            Iterator<T> iterator = items.iterator();
            if (iterator.hasNext()) {
                T item = iterator.next();
                iterator.remove();
                return Optional.of(item);
            } else {
                return Optional.empty();
            }
        });
    }

    public int size() {
        return items.size();
    }
}
