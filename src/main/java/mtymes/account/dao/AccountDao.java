package mtymes.account.dao;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Version;

import java.util.Optional;

public interface AccountDao {

    boolean createAccount(AccountId accountId, Version version);

    boolean updateBalance(AccountId accountId, Decimal newBalance, Version oldVersion, Version newVersion);

    Optional<Account> findAccount(AccountId accountId);

    Optional<Version> findVersion(AccountId accountId);
}
