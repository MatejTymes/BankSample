package mtymes.account.dao;

import mtymes.account.domain.operation.*;
import org.apache.commons.lang3.NotImplementedException;
import org.bson.Document;

public class MongoMapper implements OperationVisitor<Document> {

    // todo: test
    @Override
    public Document visit(CreateAccount request) {
        // todo: implement
        throw new NotImplementedException("implement me");
    }

    // todo: test
    @Override
    public Document visit(DepositMoney request) {
        // todo: implement
        throw new NotImplementedException("implement me");
    }

    // todo: test
    @Override
    public Document visit(WithdrawMoney request) {
        // todo: implement
        throw new NotImplementedException("implement me");
    }

    // todo: test
    @Override
    public Document visit(InternalTransfer request) {
        // todo: implement
        throw new NotImplementedException("implement me");
    }
}
