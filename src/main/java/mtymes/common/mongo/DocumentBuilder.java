package mtymes.common.mongo;

import javafixes.math.Decimal;
import javafixes.object.Microtype;
import org.bson.Document;
import org.bson.types.Decimal128;

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

    public static Document emptyDoc() {
        return docBuilder().build();
    }

    public Document build() {
        return new Document(values);
    }

    public DocumentBuilder put(String key, Object value) {
        if (value instanceof Optional) {
            ((Optional) value).ifPresent(o -> values.put(key, toValueToStore(o)));
        } else {
            values.put(key, toValueToStore(value));
        }
        return this;
    }

    private Object toValueToStore(Object value) {
        Object valueToStore = value;

        if (valueToStore instanceof Microtype) {
            valueToStore = ((Microtype) valueToStore).getValue();
        }

        if (valueToStore instanceof Decimal) {
            valueToStore = new Decimal128(((Decimal) valueToStore).bigDecimalValue());
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
