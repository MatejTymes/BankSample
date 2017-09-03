package mtymes.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import javafixes.object.Microtype;

import java.io.IOException;

public class MicrotypeSerializer extends StdSerializer<Microtype> {

    public MicrotypeSerializer() {
        super(Microtype.class);
    }

    @Override
    public void serialize(Microtype microtype, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Object value = microtype.getValue();
        JsonSerializer<Object> valueSerializer = provider.findValueSerializer(value.getClass());
        valueSerializer.serialize(value, gen, provider);
    }
}
