package mtymes.account.domain.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import javafixes.object.Microtype;

import java.util.UUID;

public class AccountId extends Microtype<UUID> {

    private AccountId(UUID value) {
        super(value);
    }

    public static AccountId accountId(UUID value) {
        return new AccountId(value);
    }

    @JsonCreator
    public static AccountId accountId(String value) {
        return new AccountId(UUID.fromString(value));
    }

    // todo: remove
    public static AccountId newAccountId() {
        return new AccountId(UUID.randomUUID());
    }
}
