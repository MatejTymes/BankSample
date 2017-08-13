package mtymes.account.domain.request;

import javafixes.object.Microtype;

public class RequestId extends Microtype<Long> implements Comparable<RequestId> {

    private RequestId(long value) {
        super(value);
    }

    public static RequestId requestId(long value) {
        return new RequestId(value);
    }

    @Override
    public int compareTo(RequestId other) {
        return Long.compare(value(), other.value());
    }
}
