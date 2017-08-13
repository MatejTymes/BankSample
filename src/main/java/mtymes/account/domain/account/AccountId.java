package mtymes.account.domain.account;

import javafixes.object.Microtype;

import java.util.UUID;

public class AccountId extends Microtype<UUID> {

    private AccountId(UUID value) {
        super(value);
    }

    public static AccountId accountId(UUID value) {
        return new AccountId(value);
    }

    public static AccountId newAccountId() {
        return new AccountId(UUID.randomUUID());
    }
}
