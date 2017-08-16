package mtymes.account.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

import java.util.function.Consumer;

import static com.mongodb.client.model.Indexes.ascending;
import static javafixes.common.CollectionUtil.newSet;

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
                operations -> operations.createIndex(
                        ascending("accountIds", "finalState")
                )
        );
    }

    private static MongoCollection<Document> getOrCreateCollection(MongoDatabase db, String collectionName, Consumer<MongoCollection<Document>> indexCreator) {
        if (newSet(db.listCollectionNames()).contains(collectionName)) {
            return db.getCollection(collectionName);
        } else {
            db.createCollection(collectionName);

            MongoCollection<Document> collection = db.getCollection(collectionName);
            indexCreator.accept(collection);
            return collection;
        }
    }
}
