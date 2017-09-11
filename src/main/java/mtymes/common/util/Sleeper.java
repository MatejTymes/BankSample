package mtymes.common.util;

import java.time.Duration;

// todo: test
public class Sleeper {

    public static final Sleeper INSTANCE = new Sleeper();

    public void sleepFor(long durationInMillis) throws InterruptedException {
        Thread.sleep(durationInMillis);
    }

    public void sleepFor(Duration duration) throws InterruptedException {
        sleepFor(duration.toMillis());
    }
}
