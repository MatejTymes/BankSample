package mtymes.account.domain.operation;

import javafixes.object.Microtype;

// todo: rename OpId -> SeqId, SeqId -> SeqId
public class SeqId extends Microtype<Long> implements Comparable<SeqId> {

    private SeqId(long value) {
        super(value);
    }

    public static SeqId seqId(long value) {
        return new SeqId(value);
    }

    @Override
    public int compareTo(SeqId other) {
        return Long.compare(value(), other.value());
    }

    public boolean isBefore(SeqId seqId) {
        return this.compareTo(seqId) < 0;
    }

}
