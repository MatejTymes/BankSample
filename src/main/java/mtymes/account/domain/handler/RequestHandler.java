package mtymes.account.domain.handler;

import mtymes.account.domain.request.Request;
import mtymes.account.domain.request.RequestId;

public interface RequestHandler<T extends Request> {

    void handleEvent(RequestId requestId, T request);
}
