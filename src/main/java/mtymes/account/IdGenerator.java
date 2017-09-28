package mtymes.account;

import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;

import static java.util.UUID.randomUUID;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.account.domain.operation.OperationId.operationId;

public class IdGenerator {

    public OperationId nextOperationId() {
        return operationId(randomUUID());
    }

    public AccountId nextAccountId() {
        return accountId(randomUUID());
    }
}
