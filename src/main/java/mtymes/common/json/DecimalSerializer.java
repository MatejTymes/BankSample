package mtymes.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import javafixes.math.Decimal;

import java.io.IOException;

public class DecimalSerializer extends StdSerializer<Decimal> {

    public DecimalSerializer() {
        super(Decimal.class);
    }

    @Override
    public void serialize(Decimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeNumber(value.toPlainString(2));
    }
}
