package mtymes.account.dao;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;

import java.util.Optional;

public interface AccountDao {

    boolean createAccount(AccountId accountId, SeqId version);

    boolean updateBalance(AccountId accountId, Decimal newBalance, SeqId oldVersion, SeqId newVersion);

    Optional<Account> findAccount(AccountId accountId);

    Optional<SeqId> findCurrentVersion(AccountId accountId);
}
