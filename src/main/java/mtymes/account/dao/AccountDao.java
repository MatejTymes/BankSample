package mtymes.account.dao;

import com.mongodb.Function;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import org.bson.Document;

import java.math.BigDecimal;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static mtymes.account.domain.operation.OperationId.operationId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class AccountDao {

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
    public void markOperationWasUpdated(AccountId accountId, OperationId operationId) {
        accounts.updateOne(
                docBuilder()
                        .put("accountId", accountId)
                        .put("operationId", operationId)
                        .build(),
                doc("$set", doc("updatedEvent", true))
        );
    }


    // todo: test
    public Optional<OperationId> findLastAppliedOperationId(AccountId accountId) {
        return findOne(
                accounts,
                doc("accountId", accountId),
                doc -> operationId(doc.getLong("operationId"))
        );
    }

    // todo: move into BaseDao
    protected <T> Optional<T> findOne(MongoCollection<Document> collection, Document query, Function<Document, T> mapper) {
        MongoCursor<Document> iterator = collection.find(query).iterator();
        if (iterator.hasNext()) {
            Document dbItem = iterator.next();
            if (iterator.hasNext()) {
                throw new IllegalStateException("more than one db item for query: " + query);
            }

            return Optional.of(mapper.apply(dbItem));
        } else {
            return Optional.empty();
        }
    }
}
