package mtymes.account.domain.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import javafixes.object.Microtype;

public class Version extends Microtype<Long> implements Comparable<Version> {

    private Version(Long value) {
        super(value);
    }

    @JsonCreator
    public static final Version version(long value) {
        return new Version(value);
    }

    @Override
    public int compareTo(Version other) {
        return Long.compare(value(), other.value());
    }

    public boolean isBefore(Version version) {
        return this.compareTo(version) < 0;
    }
}
