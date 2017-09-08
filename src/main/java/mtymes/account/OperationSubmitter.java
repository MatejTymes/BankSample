package mtymes.account;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.Failure;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.work.Worker;
import mtymes.common.util.Either;

import java.util.Optional;

import static java.lang.String.format;
import static mtymes.account.domain.Failure.failure;
import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.domain.operation.TransferId.newTransferId;
import static mtymes.common.util.Either.left;
import static mtymes.common.util.Either.right;

// todo: test this
public class OperationSubmitter {

    private final AccountDao accountDao;
    private final OperationDao operationDao;
    private final Worker worker;

    public OperationSubmitter(AccountDao accountDao, OperationDao operationDao, Worker worker) {
        this.accountDao = accountDao;
        this.operationDao = operationDao;
        this.worker = worker;
    }

    public Either<Failure, Account> createAccount() {
        AccountId accountId = newAccountId();
        LoggedOperation operation = submitOperation(new CreateAccount(accountId));

        if (wasOperationApplied(operation)) {
            return accountDao
                    .findAccount(accountId)
                    .map(this::asResponse)
                    .orElseGet(() -> asFailure(format("Failed to load created Account '%s'", accountId)));
        } else {
            return asFailure(operation.description.orElse("Failed to finish this operation"));
        }
    }

    public Either<Failure, FinalState> depositMoney(AccountId accountId, Decimal amount) {
        LoggedOperation operation = submitOperation(new DepositTo(accountId, amount));
        return asStateResponse(operation);
    }

    public Either<Failure, FinalState> withdrawMoney(AccountId accountId, Decimal amount) {
        LoggedOperation operation = submitOperation(new WithdrawFrom(accountId, amount));
        return asStateResponse(operation);
    }

    public Either<Failure, FinalState> transferMoney(AccountId fromAccountId, AccountId toAccountId, Decimal amount) {
        LoggedOperation operation = submitOperation(new TransferFrom(new TransferDetail(newTransferId(), fromAccountId, toAccountId, amount)));
        return asStateResponse(operation);
    }

    /* ========================== */
    /* ---   helper methods   --- */
    /* ========================== */

    private LoggedOperation submitOperation(Operation operation) {
        OpLogId opLogId = operationDao.storeOperation(operation);
        worker.runUnfinishedOperations(opLogId.accountId);
        return operationDao.findLoggedOperation(opLogId).get();
    }

    private boolean wasOperationApplied(LoggedOperation operation) {
        Optional<FinalState> finalState = operation.finalState;
        return finalState.isPresent() && finalState.get() == FinalState.Applied;
    }

    private <T> Either<Failure, T> asResponse(T object) {
        return right(object);
    }

    private <T> Either<Failure, T> asFailure(String message) {
        return left(failure(message));
    }

    private Either<Failure, FinalState> asStateResponse(LoggedOperation operation) {
        return operation.finalState
                .map(this::asResponse)
                .orElseGet(() -> asFailure("Failed to finish this operation"));
    }
}
