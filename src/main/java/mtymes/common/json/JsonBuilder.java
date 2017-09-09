package mtymes.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafixes.math.Decimal;

import java.math.BigDecimal;

public class JsonBuilder {

    private ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);

    public static JsonBuilder jsonBuilder() {
        return new JsonBuilder();
    }

    public static ObjectNode jsonObject(String fieldName, Object value) {
        return jsonBuilder().with(fieldName, value).build();
    }

    public static ObjectNode emptyJson() {
        return jsonBuilder().build();
    }

    public ObjectNode build() {
        // we return a copy so the builder can be reused without affecting the returned object
        ObjectNode objectNodeCopy = new ObjectNode(JsonNodeFactory.instance);
        objectNodeCopy.setAll(objectNode);
        return objectNodeCopy;
    }

    public String buildString() {
        return objectNode.toString();
    }

    public JsonBuilder with(String fieldName, Object value) {
        if (value == null) {
            objectNode.putNull(fieldName);
        } else if (value instanceof JsonNode) {
            objectNode.set(fieldName, (JsonNode) value);
        } else if (value instanceof String) {
            objectNode.put(fieldName, (String) value);
        } else if (value instanceof Decimal) {
            BigDecimal bigDecimal = ((Decimal) value).bigDecimalValue();
            if (bigDecimal.scale() < 2) {
                bigDecimal = bigDecimal.setScale(2, BigDecimal.ROUND_UNNECESSARY);
            }
            objectNode.put(fieldName, bigDecimal);
        } else if (value instanceof Integer) {
            objectNode.put(fieldName, (Integer) value);
        } else {
            objectNode.putPOJO(fieldName, value);
        }
        return this;
    }
}
