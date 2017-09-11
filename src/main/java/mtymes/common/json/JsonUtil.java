package mtymes.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import javafixes.math.Decimal;
import javafixes.object.Microtype;

import java.io.IOException;

public class JsonUtil {

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();

        mapper.registerModule(new AfterburnerModule());
        mapper.registerModule(new Jdk8Module());

        SimpleModule customModule = new SimpleModule();
        customModule.addSerializer(Decimal.class, new DecimalSerializer());
        customModule.addDeserializer(Decimal.class, new DecimalDeserializer());
        customModule.addSerializer(Microtype.class, new MicrotypeSerializer());
        mapper.registerModule(customModule);
    }

    public static String toJsonString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ObjectNode toJsonObject(String jsonString) {
        try {
            return (ObjectNode) mapper.reader().readTree(jsonString);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T toObject(String jsonString, Class<T> responseType) {
        try {
            return mapper.readerFor(responseType).readValue(jsonString);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
