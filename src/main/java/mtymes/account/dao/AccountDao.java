package mtymes.account.dao;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;

import java.util.Optional;

public interface AccountDao {

    boolean createAccount(AccountId accountId, SeqId seqId);

    // todo: check that the fromSeqId < toSeqId
    boolean updateBalance(AccountId accountId, Decimal newBalance, SeqId fromSeqId, SeqId toSeqId);

    Optional<Account> findAccount(AccountId accountId);

    // todo: test
    Optional<SeqId> findLastAppliedOperationId(AccountId accountId);
}
