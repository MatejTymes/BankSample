package mtymes.account.dao;

import com.mongodb.Function;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

import java.util.Optional;

public abstract class BaseDao {

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
