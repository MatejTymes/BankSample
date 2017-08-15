package mtymes.common.mongo;

import javafixes.object.Microtype;
import org.bson.Document;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.stream.Collectors.toList;
import static javafixes.common.StreamUtil.toStream;

// todo: test this
public class DocumentBuilder {

    private final Map<String, Object> values = newLinkedHashMap();

    public static DocumentBuilder docBuilder() {
        return new DocumentBuilder();
    }

    public static Document doc(String key, Object value) {
        return docBuilder().put(key, value).build();
    }

    public Document build() {
        return new Document(values);
    }

    public DocumentBuilder put(String key, Object value) {
        values.put(key, toValueToStore(value));
        return this;
    }

    public DocumentBuilder put(String key, Optional<?> value) {
        value.ifPresent(o -> put(key, o));
        return this;
    }

    private Object toValueToStore(Object value) {
        Object valueToStore = value;

        if (valueToStore instanceof Microtype) {
            valueToStore = ((Microtype) valueToStore).getValue();
        }

        // todo: do we still need this ???
        if (valueToStore instanceof BigDecimal) {
            valueToStore = ((BigDecimal) valueToStore).doubleValue();
        } else if (valueToStore instanceof UUID) {
            valueToStore = ((UUID) valueToStore).toString();
        } else if (valueToStore instanceof Iterable) {
            valueToStore = toStream((Iterable) valueToStore)
                    .map(this::toValueToStore)
                    .collect(toList());
        }

        return valueToStore;
    }
}
