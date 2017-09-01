package mtymes.account.domain.operation;

import javafixes.object.Microtype;

import java.util.UUID;

public class TransferId extends Microtype<UUID> {

    private TransferId(UUID value) {
        super(value);
    }

    public static TransferId transferId(UUID value) {
        return new TransferId(value);
    }

    public static TransferId newTransferId() {
        return new TransferId(UUID.randomUUID());
    }
}
