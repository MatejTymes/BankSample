package mtymes.account.dao.mongo;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.util.UUID;

import static java.lang.String.format;
import static javafixes.math.Decimal.d;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.account.domain.operation.TransferId.transferId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;


public class OperationDbMapper implements OperationVisitor<Document> {

    private static final String ACCOUNT_ID = "accountId";
    private static final String AMOUNT = "amount";
    private static final String FROM_ACCOUNT_ID = "fromAccountId";
    private static final String TO_ACCOUNT_ID = "toAccountId";
    public static final String TRANSFER_ID = "transferId";

    @Override
    public Document visit(CreateAccount request) {
        return doc(ACCOUNT_ID, request.accountId);
    }

    @Override
    public Document visit(DepositTo request) {
        return docBuilder()
                .put(ACCOUNT_ID, request.accountId)
                .put(AMOUNT, request.amount)
                .build();
    }

    @Override
    public Document visit(WithdrawFrom request) {
        return docBuilder()
                .put(ACCOUNT_ID, request.accountId)
                .put(AMOUNT, request.amount)
                .build();
    }

    @Override
    public Document visit(TransferFrom request) {
        return toDocument(request.detail);
    }

    @Override
    public Document visit(TransferTo request) {
        return toDocument(request.detail);
    }

    public Operation toOperation(String type, Document body) {
        switch (type) {
            case "CreateAccount":
                return new CreateAccount(
                        getAccountId(body, ACCOUNT_ID)
                );
            case "DepositTo":
                return new DepositTo(
                        getAccountId(body, ACCOUNT_ID),
                        getDecimal(body, AMOUNT)
                );
            case "WithdrawFrom":
                return new WithdrawFrom(
                        getAccountId(body, ACCOUNT_ID),
                        getDecimal(body, AMOUNT)
                );
            case "TransferFrom":
                return new TransferFrom(
                        toTransferDetail(body)
                );
            case "TransferTo":
                return new TransferTo(
                        toTransferDetail(body)
                );
        }
        throw new IllegalStateException(format("Unknown type '%s'", type));
    }

    private Document toDocument(TransferDetail details) {
        return docBuilder()
                .put(TRANSFER_ID, details.transferId)
                .put(FROM_ACCOUNT_ID, details.fromAccountId)
                .put(TO_ACCOUNT_ID, details.toAccountId)
                .put(AMOUNT, details.amount)
                .build();
    }

    private TransferDetail toTransferDetail(Document body) {
        return new TransferDetail(
                getTransferId(body, TRANSFER_ID),
                getAccountId(body, FROM_ACCOUNT_ID),
                getAccountId(body, TO_ACCOUNT_ID),
                getDecimal(body, AMOUNT)
        );
    }

    public AccountId getAccountId(Document body, String fieldName) {
        return accountId(getUUID(body, fieldName));
    }

    public TransferId getTransferId(Document body, String fieldName) {
        return transferId(getUUID(body, fieldName));
    }

    public Decimal getDecimal(Document body, String fieldName) {
        return d(((Decimal128) body.get(fieldName)).bigDecimalValue());
    }

    public UUID getUUID(Document body, String fieldName) {
        return UUID.fromString(body.getString(fieldName));
    }
}
