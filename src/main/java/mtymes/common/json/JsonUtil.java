package mtymes.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import javafixes.math.Decimal;
import javafixes.object.Microtype;

public class JsonUtil {

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();

        mapper.registerModule(new AfterburnerModule());
        mapper.registerModule(new Jdk8Module());

        SimpleModule customModule = new SimpleModule();
        customModule.addSerializer(Decimal.class, new DecimalSerializer());
//        customModule.addDeserializer(Decimal.class, new DecimalDeserializer());
        customModule.addSerializer(Microtype.class, new MicrotypeSerializer());
        mapper.registerModule(customModule);
    }

    // todo: test
    public static String toJson(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }
}
