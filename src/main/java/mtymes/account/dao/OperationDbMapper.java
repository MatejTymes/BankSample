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

    private static final String ACCOUNT_ID = "accountId";
    private static final String AMOUNT = "amount";
    private static final String FROM_ACCOUNT_ID = "fromAccountId";
    private static final String TO_ACCOUNT_ID = "toAccountId";

    // todo: test
    @Override
    public Document visit(CreateAccount request) {
        return doc(ACCOUNT_ID, request.accountId);
    }

    // todo: test
    @Override
    public Document visit(DepositMoney request) {
        return docBuilder()
                .put(ACCOUNT_ID, request.accountId)
                .put(AMOUNT, request.amount)
                .build();
    }

    // todo: test
    @Override
    public Document visit(WithdrawMoney request) {
        return docBuilder()
                .put(ACCOUNT_ID, request.accountId)
                .put(AMOUNT, request.amount)
                .build();
    }

    // todo: test
    @Override
    public Document visit(InternalTransfer request) {
        return docBuilder()
                .put(FROM_ACCOUNT_ID, request.fromAccountId)
                .put(TO_ACCOUNT_ID, request.toAccountId)
                .put(AMOUNT, request.amount)
                .build();
    }

    // todo: test
    public Operation toOperation(String type, Document body) {
        switch (type) {
            case "CreateAccount":
                return new CreateAccount(
                        getAccountId(body, ACCOUNT_ID)
                );
            case "DepositMoney":
                return new DepositMoney(
                        getAccountId(body, ACCOUNT_ID),
                        getDecimal(body, AMOUNT)
                );
            case "WithdrawMoney":
                return new WithdrawMoney(
                        getAccountId(body, ACCOUNT_ID),
                        getDecimal(body, AMOUNT)
                );
            case "InternalTransfer":
                return new InternalTransfer(
                        getAccountId(body, FROM_ACCOUNT_ID),
                        getAccountId(body, TO_ACCOUNT_ID),
                        getDecimal(body, AMOUNT)
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
