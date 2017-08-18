package mtymes.account.dao;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.util.UUID;

import static java.lang.String.format;
import static javafixes.math.Decimal.d;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;


public class OperationDbMapper implements OperationVisitor<Document> {

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

    // todo: test
    public Operation toOperation(String type, Document body) {
        switch (type) {
            case "CreateAccount":
                return new CreateAccount(
                        getAccountId(body, "accountId")
                );
            case "DepositMoney":
                return new DepositMoney(
                        getAccountId(body, "accountId"),
                        getDecimal(body, "amount")
                );
            case "WithdrawMoney":
                return new WithdrawMoney(
                        getAccountId(body, "accountId"),
                        getDecimal(body, "amount")
                );
            case "InternalTransfer":
                return new InternalTransfer(
                        getAccountId(body, "fromAccountId"),
                        getAccountId(body, "toAccountId"),
                        getDecimal(body, "amount")
                );
        }
        throw new IllegalStateException(format("Unknown type '%s'", type));
    }

    private AccountId getAccountId(Document body, String fieldName) {
        return accountId(UUID.fromString(body.getString(fieldName)));
    }

    private Decimal getDecimal(Document body, String fieldName) {
        return d(((Decimal128)body.get(fieldName)).bigDecimalValue());
    }
}
