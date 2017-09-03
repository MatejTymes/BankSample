package mtymes.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import javafixes.math.Decimal;

import java.io.IOException;

import static javafixes.math.Decimal.decimal;

public class DecimalDeserializer extends StdDeserializer<Decimal> {

    public DecimalDeserializer() {
        super(Decimal.class);
    }

    @Override
    public Decimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return decimal(p.getValueAsString());
    }
}
