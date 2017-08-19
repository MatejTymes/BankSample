package mtymes.account.dao;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;

import java.util.Optional;

public interface AccountDao {

    boolean createAccount(AccountId accountId, OperationId operationId);

    // todo: check that the fromOperationId < toOperationId
    boolean updateBalance(AccountId accountId, Decimal newBalance, OperationId fromOperationId, OperationId toOperationId);

    Optional<Account> findAccount(AccountId accountId);

    // todo: test
    Optional<OperationId> findLastAppliedOperationId(AccountId accountId);
}
