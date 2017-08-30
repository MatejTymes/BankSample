package mtymes.account.dao.mongo;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static javafixes.math.Decimal.d;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.account.domain.operation.TransferId.transferId;
import static mtymes.account.domain.operation.Version.version;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;


public class MongoMapper implements OperationVisitor<Document> {

    public static final String ACCOUNT_ID = "accountId";
    public static final String AMOUNT = "amount";
    public static final String FROM_ACCOUNT_ID = "fromAccountId";
    public static final String TO_ACCOUNT_ID = "toAccountId";
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

    private TransferDetail toTransferDetail(Document doc) {
        return new TransferDetail(
                getTransferId(doc, TRANSFER_ID),
                getAccountId(doc, FROM_ACCOUNT_ID),
                getAccountId(doc, TO_ACCOUNT_ID),
                getDecimal(doc, AMOUNT)
        );
    }

    public AccountId getAccountId(Document doc, String fieldName) {
        return accountId(getUUID(doc, fieldName));
    }

    public TransferId getTransferId(Document doc, String fieldName) {
        return transferId(getUUID(doc, fieldName));
    }

    public Version getVersion(Document doc, String fieldName) {
        return version(doc.getLong(fieldName));
    }

    public Optional<FinalState> getOptionalFinalState(Document doc, String fieldName) {
        return Optional.ofNullable(doc.getString(fieldName)).map(FinalState::valueOf);
    }

    public Decimal getDecimal(Document doc, String fieldName) {
        return d(((Decimal128) doc.get(fieldName)).bigDecimalValue());
    }

    public UUID getUUID(Document doc, String fieldName) {
        return UUID.fromString(doc.getString(fieldName));
    }

    public Optional<String> getOptionalString(Document doc, String fieldName) {
        return Optional.ofNullable(doc.getString(fieldName));
    }
}
