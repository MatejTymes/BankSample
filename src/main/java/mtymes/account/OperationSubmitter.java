package mtymes.account;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.account.work.Worker;
import mtymes.common.domain.Failure;
import mtymes.common.domain.Success;
import mtymes.common.util.Either;

import java.util.Optional;

import static java.lang.String.format;
import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.TransferId.newTransferId;
import static mtymes.common.domain.Failure.failure;
import static mtymes.common.util.Either.left;
import static mtymes.common.util.Either.right;

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

    public Either<Failure, Success> depositMoney(AccountId accountId, Decimal amount) {
        LoggedOperation operation = submitOperation(new DepositTo(accountId, amount));
        return asResponse(operation);
    }

    public Either<Failure, Success> withdrawMoney(AccountId accountId, Decimal amount) {
        LoggedOperation operation = submitOperation(new WithdrawFrom(accountId, amount));
        return asResponse(operation);
    }

    public Either<Failure, Success> transferMoney(AccountId fromAccountId, AccountId toAccountId, Decimal amount) {
        LoggedOperation operation = submitOperation(new TransferFrom(new TransferDetail(newTransferId(), fromAccountId, toAccountId, amount)));
        return asResponse(operation);
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
