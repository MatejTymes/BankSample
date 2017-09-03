package mtymes.test;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;

import java.util.Optional;
import java.util.function.Supplier;

public class BrokenAccountDao extends BrokenClass implements AccountDao {

    private final AccountDao wrappedDao;

    public BrokenAccountDao(AccountDao wrappedDao, Supplier<RuntimeException> exceptionSupplier) {
        super(exceptionSupplier);
        this.wrappedDao = wrappedDao;
    }

    @Override
    public boolean createAccount(AccountId accountId, Version version) {
        failTheFirstTime("createAccount", accountId, version);
        return wrappedDao.createAccount(accountId, version);
    }

    @Override
    public boolean updateBalance(AccountId accountId, Decimal newBalance, Version oldVersion, Version newVersion) {
        failTheFirstTime("updateBalance", accountId, newBalance, oldVersion, newVersion);
        return wrappedDao.updateBalance(accountId, newBalance, oldVersion, newVersion);
    }

    @Override
    public Optional<Account> findAccount(AccountId accountId) {
        failTheFirstTime("findAccount", accountId);
        return wrappedDao.findAccount(accountId);
    }

    @Override
    public Optional<Version> findCurrentVersion(AccountId accountId) {
        failTheFirstTime("findCurrentVersion", accountId);
        return wrappedDao.findCurrentVersion(accountId);
    }
}
