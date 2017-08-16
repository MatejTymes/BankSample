package mtymes.account.dao;

import mtymes.account.domain.operation.*;
import mtymes.common.mongo.DocumentBuilder;
import org.apache.commons.lang3.NotImplementedException;
import org.bson.Document;

import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;


public class MongoMapper implements OperationVisitor<Document> {

    // todo: test
    @Override
    public Document visit(CreateAccount request) {
        return doc("accountId", request.accountId);
    }

    // todo: test
    @Override
    public Document visit(DepositMoney request) {
        return docBuilder()
                .put("accountId", request.accountId)
                .put("amount", request.amount)
                .build();
    }

    // todo: test
    @Override
    public Document visit(WithdrawMoney request) {
        return docBuilder()
                .put("accountId", request.accountId)
                .put("amount", request.amount)
                .build();
    }

    // todo: test
    @Override
    public Document visit(InternalTransfer request) {
        return docBuilder()
                .put("fromAccountId", request.fromAccountId)
                .put("toAccountId", request.toAccountId)
                .put("amount", request.amount)
                .build();
    }
}
