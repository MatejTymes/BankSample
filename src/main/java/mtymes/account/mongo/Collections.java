package mtymes.account.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

import java.util.function.Consumer;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.descending;
import static javafixes.common.CollectionUtil.newSet;
import static mtymes.common.mongo.DocumentBuilder.doc;

public class Collections {

    public static MongoCollection<Document> accountsCollection(MongoDatabase database) {
        return getOrCreateCollection(
                database,
                "accounts",
                accounts -> accounts.createIndex(
                        ascending("accountId"),
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
                            descending("accountIds", "finalState")
                    );
                    operations.createIndex(
                            ascending("transferId", "type"),
                            new IndexOptions().unique(true).partialFilterExpression(doc("transferId", doc("$exists", true)))
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
