package mtymes.account.dao.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

import java.util.function.Consumer;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.descending;
import static javafixes.common.CollectionUtil.newSet;

public class Collections {

    public static MongoCollection<Document> accountsCollection(MongoDatabase database) {
        return getOrCreateCollection(
                database,
                "accounts",
                accounts -> accounts.createIndex(
                        ascending(
                                MongoAccountDao.ACCOUNT_ID
                        ),
                        new IndexOptions().unique(true)
                )
        );
    }

    public static MongoCollection<Document> opLogCollection(MongoDatabase database) {
        return getOrCreateCollection(
                database,
                "opLogs",
                opLogs -> {
                    opLogs.createIndex(
                            descending(
                                    MongoOpLogDao.ACCOUNT_ID,
                                    MongoOpLogDao.SEQ_ID
                            ),
                            new IndexOptions().unique(true)
                    );
                    opLogs.createIndex(
                            ascending(
                                    MongoOpLogDao.OPERATION_ID
                            ),
                            new IndexOptions().unique(true)
                    );
                }
        );
    }

    public static MongoCollection<Document> operationsCollection(MongoDatabase database) {
        return getOrCreateCollection(
                database,
                "operations",
                operations -> {
                    // todo: test this
                    operations.createIndex(
                            ascending(
                                    MongoOperationDao.OPERATION_ID
                            ),
                            new IndexOptions().unique(true)
                    );
                }
        );
    }

    private static MongoCollection<Document> getOrCreateCollection(MongoDatabase database, String collectionName, Consumer<MongoCollection<Document>> afterCreation) {
        if (!newSet(database.listCollectionNames()).contains(collectionName)) {
            database.createCollection(collectionName);

            MongoCollection<Document> collection = database.getCollection(collectionName);
            afterCreation.accept(collection);
            return collection;
        } else {
            return database.getCollection(collectionName);
        }
    }
}
