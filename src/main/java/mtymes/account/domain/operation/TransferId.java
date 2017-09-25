package mtymes.account.domain.operation;

import com.fasterxml.jackson.annotation.JsonCreator;
import javafixes.object.Microtype;

import java.util.UUID;

public class TransferId extends Microtype<UUID> {

    private TransferId(UUID value) {
        super(value);
    }

    public static TransferId transferId(UUID value) {
        return new TransferId(value);
    }

    @JsonCreator
    public static TransferId transferId(String value) {
        return new TransferId(UUID.fromString(value));
    }

    // todo: remove
    public static TransferId newTransferId() {
        return new TransferId(UUID.randomUUID());
    }
}
