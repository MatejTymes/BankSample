package mtymes.account.domain.operation;

import com.fasterxml.jackson.annotation.JsonCreator;
import javafixes.object.Microtype;

public class SeqId extends Microtype<Long> implements Comparable<SeqId> {

    private SeqId(long value) {
        super(value);
    }

    @JsonCreator
    public static SeqId seqId(long value) {
        return new SeqId(value);
    }

    @Override
    public int compareTo(SeqId other) {
        return Long.compare(value(), other.value());
    }

    public boolean isBefore(SeqId other) {
        return this.compareTo(other) < 0;
    }

    public boolean canApplyAfter(SeqId accountVersion) {
        return accountVersion.isBefore(this);
    }

    public boolean isCurrentlyApplied(SeqId accountVersion) {
        return accountVersion.equals(this);
    }
}
