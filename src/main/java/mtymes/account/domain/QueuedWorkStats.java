package mtymes.account.domain;

public class QueuedWorkStats {

    public final int queuedCount;
    public final int inProgressCount;

    public QueuedWorkStats(int queuedCount, int inProgressCount) {
        this.queuedCount = queuedCount;
        this.inProgressCount = inProgressCount;
    }

    @SuppressWarnings("unused")
    private QueuedWorkStats() {
        this.queuedCount = -1;
        this.inProgressCount = -1;
    }
}
