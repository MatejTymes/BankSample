package mtymes.account.dao;

import com.mongodb.Function;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Operation;
import mtymes.account.domain.operation.OperationId;
import org.bson.Document;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.account.domain.operation.OperationId.operationId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class AccountDao extends BaseDao {

    private final MongoCollection<Document> accounts;

    public AccountDao(MongoCollection<Document> accounts) {
        this.accounts = accounts;
    }

    // todo: test
    public boolean createAccount(AccountId accountId, OperationId operationId) {
        try {
            accounts.insertOne(docBuilder()
                    .put("accountId", accountId)
                    .put("lastAppliedOpId", operationId)
                    .put("balance", BigDecimal.ZERO)
                    .build());
            return true;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    // todo: test
    public boolean updateBalance(AccountId accountId, BigDecimal newBalance, OperationId fromOperationId, OperationId toOperationId) {
        try {
            accounts.updateOne(
                    docBuilder()
                            .put("accountId", accountId)
                            .put("lastAppliedOpId", fromOperationId)
                            .build(),
                    doc("$set", docBuilder()
                            .put("balance", newBalance)
                            .put("lastAppliedOpId", toOperationId)
                            .build())
            );
            return true;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    // todo: test
    public Optional<Account> findAccount(AccountId accountId) {
        return findOne(
                accounts,
                doc("accountId", accountId),
                doc -> new Account(
                        accountId(UUID.fromString(doc.getString("accountId"))),
                        BigDecimal.valueOf(doc.getDouble("balance")),
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
