package mtymes.account.dao;

import mtymes.account.domain.operation.FinalState;
import mtymes.account.domain.operation.OperationId;
import org.apache.commons.lang3.NotImplementedException;

public class RequestDao {

    // todo: test
    public void markAsSuccessful(OperationId operationId) {
        // todo: implement
        throw new NotImplementedException("implement me");
    }

    // todo: test
    public void markAsFailed(OperationId operationId, String description) {
        // todo: implement
        throw new NotImplementedException("implement me");
    }
}
