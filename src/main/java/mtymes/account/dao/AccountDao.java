package mtymes.account.dao;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Version;

import java.util.Optional;

public interface AccountDao {

    boolean createAccount(AccountId accountId, Version version);

    // todo: check that the fromVersion < toVersion
    boolean updateBalance(AccountId accountId, Decimal newBalance, Version fromVersion, Version toVersion);

    Optional<Account> findAccount(AccountId accountId);

    // todo: test
    Optional<Version> findVersion(AccountId accountId);
}
