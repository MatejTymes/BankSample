package mtymes.account.dao;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.util.Optional;
import java.util.UUID;

import static javafixes.math.Decimal.d;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.account.domain.operation.OperationId.operationId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class AccountDao extends BaseDao {

    private final MongoCollection<Document> accounts;

    public AccountDao(MongoCollection<Document> accounts) {
        this.accounts = accounts;
    }

    public boolean createAccount(AccountId accountId, OperationId operationId) {
        try {
            accounts.insertOne(docBuilder()
                    .put("accountId", accountId)
                    .put("lastAppliedOpId", operationId)
                    .put("balance", Decimal.ZERO)
                    .build());
            return true;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    public boolean updateBalance(AccountId accountId, Decimal newBalance, OperationId fromOperationId, OperationId toOperationId) {
        try {
            UpdateResult result = accounts.updateOne(
                    docBuilder()
                            .put("accountId", accountId)
                            .put("lastAppliedOpId", fromOperationId)
                            .build(),
                    doc("$set", docBuilder()
                            .put("balance", newBalance)
                            .put("lastAppliedOpId", toOperationId)
                            .build())
            );
            return result.getModifiedCount() == 1;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    public Optional<Account> findAccount(AccountId accountId) {
        return findOne(
                accounts,
                doc("accountId", accountId),
                doc -> new Account(
                        accountId(UUID.fromString(doc.getString("accountId"))),
                        d(((Decimal128)doc.get("balance")).bigDecimalValue()),
                        operationId(doc.getLong("lastAppliedOpId"))
                )
        );
    }

    // todo: test
    public Optional<OperationId> findLastAppliedOperationId(AccountId accountId) {
        return findOne(
                accounts,
                doc("accountId", accountId),
                doc -> operationId(doc.getLong("lastAppliedOpId"))
        );
    }
}
