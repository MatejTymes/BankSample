package mtymes.account;

import javafixes.math.Decimal;
import javafixes.object.Either;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OpLogDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.work.Worker;
import mtymes.common.domain.Failure;
import mtymes.common.domain.Success;

import java.util.Optional;

import static java.lang.String.format;
import static javafixes.object.Either.left;
import static javafixes.object.Either.right;
import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.common.domain.Failure.failure;

public class OperationSubmitter {

    private final IdGenerator idGenerator;
    private final AccountDao accountDao;
    private final OperationDao operationDao;
    private final OpLogDao opLogDao;
    private final Worker worker;

    public OperationSubmitter(IdGenerator idGenerator, AccountDao accountDao, OperationDao operationDao, OpLogDao opLogDao, Worker worker) {
        this.idGenerator = idGenerator;
        this.accountDao = accountDao;
        this.operationDao = operationDao;
        this.opLogDao = opLogDao;
        this.worker = worker;
    }

    public Either<Failure, Account> createAccount() {
        OperationId operationId = idGenerator.nextOperationId();
        AccountId accountId = idGenerator.nextAccountId();
        LoggedOperation operation = submitOperation(new CreateAccount(operationId, accountId));

        if (wasOperationApplied(operation)) {
            return accountDao
                    .findAccount(accountId)
                    .map(this::asResponse)
                    .orElseGet(() -> asFailure(format("Failed to load created Account '%s'", accountId)));
        } else {
            return asFailure(operation.description.orElse("Failed to finish this operation"));
        }
    }

    public Either<Failure, Success> depositMoney(AccountId accountId, Decimal amount) {
        OperationId operationId = idGenerator.nextOperationId();
        LoggedOperation operation = submitOperation(new DepositTo(operationId, accountId, amount));
        return asResponse(operation);
    }

    public Either<Failure, Success> withdrawMoney(AccountId accountId, Decimal amount) {
        OperationId operationId = idGenerator.nextOperationId();
        LoggedOperation operation = submitOperation(new WithdrawFrom(operationId, accountId, amount));
        return asResponse(operation);
    }

    public Either<Failure, Success> transferMoney(AccountId fromAccountId, AccountId toAccountId, Decimal amount) {
        OperationId operationId = idGenerator.nextOperationId();
        OperationId toPartOperationId = idGenerator.nextOperationId();
        LoggedOperation operation = submitOperation(new TransferFrom(operationId, toPartOperationId, new TransferDetail(fromAccountId, toAccountId, amount)));
        return asResponse(operation);
    }

    /* ========================== */
    /* ---   helper methods   --- */
    /* ========================== */

    private LoggedOperation submitOperation(Operation operation) {
        AccountId accountId = operation.affectedAccountId();

        operationDao.storeOperation(operation);
        opLogDao.registerOperationId(accountId, operation.operationId);
        worker.runUnfinishedOperations(accountId);

        return operationDao.findLoggedOperation(operation.operationId).get();
    }

    private boolean wasOperationApplied(LoggedOperation operation) {
        Optional<FinalState> finalState = operation.finalState;
        return finalState.isPresent() && finalState.get() == Applied;
    }

    private Either<Failure, Account> asResponse(Account account) {
        return right(account);
    }

    private Either<Failure, Success> asResponse(LoggedOperation operation) {
        if (operation.finalState.map(value -> value == Applied).orElse(false)) {
            return right(new Success());
        } else {
            return left(failure(
                    operation.description.orElse("Failed to finish operation")
            ));
        }
    }

    private <T> Either<Failure, T> asFailure(String message) {
        return left(failure(message));
    }
}
