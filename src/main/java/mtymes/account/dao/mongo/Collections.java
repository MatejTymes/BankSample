package mtymes.account.dao.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

import java.util.function.Consumer;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.descending;
import static javafixes.common.CollectionUtil.newSet;
import static mtymes.account.dao.mongo.MongoMapper.TRANSFER_ID;
import static mtymes.account.dao.mongo.MongoOperationDao.BODY;
import static mtymes.common.mongo.DocumentBuilder.doc;

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

    public static MongoCollection<Document> operationsCollection(MongoDatabase database) {
        return getOrCreateCollection(
                database,
                "operations",
                operations -> {
                    operations.createIndex(
                            ascending(
                                    MongoOperationDao.ACCOUNT_ID,
                                    MongoOperationDao.FINAL_STATE,
                                    MongoOperationDao.VERSION
                            )
                    );
                    operations.createIndex(
                            ascending(
                                    MongoOperationDao.BODY + "." + MongoMapper.OPERATION_ID // todo: test this
                            ),
                            new IndexOptions().unique(true)
                    );
                    operations.createIndex(
                            descending(
                                    MongoOperationDao.ACCOUNT_ID,
                                    MongoOperationDao.VERSION
                            ),
                            new IndexOptions().unique(true)
                    );
                    // todo: remove this
                    operations.createIndex(
                            ascending(
                                    MongoOperationDao.BODY + "." + MongoMapper.TRANSFER_ID,
                                    MongoOperationDao.TYPE
                            ),
                            new IndexOptions().unique(true).partialFilterExpression(doc(BODY + "." + TRANSFER_ID, doc("$exists", true)))
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
