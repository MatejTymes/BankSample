package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.WithdrawFrom;
import org.junit.Before;
import org.junit.Test;

import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.test.ConcurrencyUtil.runConcurrentlyOnNThreads;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class WithdrawFromHandlerConcurrencyTest extends BaseOperationHandlerStabilityTest {

    private WithdrawFromHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new WithdrawFromHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToWithdrawFromOnConcurrentExecution() {
        Decimal amount = randomPositiveAmount();

        Decimal initialBalance = pickRandomValue(amount, amount.plus(randomPositiveAmount()));
        AccountId accountId = createAccountWithInitialBalance(initialBalance).accountId;

        OperationId operationId = randomOperationId();
        WithdrawFrom withdrawFrom = new WithdrawFrom(operationId, accountId, amount);
        operationDao.storeOperation(withdrawFrom);
        SeqId seqId = opLogDao.registerOperationId(accountId, operationId);

        // When
        runConcurrentlyOnNThreads(
                () -> handler.handleOperation(seqId, withdrawFrom),
                50
        );

        // Then
        LoggedOperation operation = loadOperation(operationId);
        assertThat(operation.finalState, isPresentAndEqualTo(Applied));
        assertThat(operation.description, isNotPresent());
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance.minus(amount), seqId)));
    }

    @Test
    public void shouldFailToWithdrawFromIfThereIsInsufficientBalanceOnConcurrentExecution() {
        Decimal initialBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        Account initialAccount = createAccountWithInitialBalance(initialBalance);
        AccountId accountId = initialAccount.accountId;

        Decimal amount = initialBalance.signum() >= 0 ? initialBalance.plus(randomPositiveAmount()) : randomPositiveAmount();
        OperationId operationId = randomOperationId();
        WithdrawFrom withdrawFrom = new WithdrawFrom(operationId, accountId, amount);
        operationDao.storeOperation(withdrawFrom);
        SeqId seqId = opLogDao.registerOperationId(accountId, operationId);

        // When
        runConcurrentlyOnNThreads(
                () -> handler.handleOperation(seqId, withdrawFrom),
                50
        );

        // Then
        LoggedOperation operation = loadOperation(operationId);
        assertThat(operation.finalState, isPresentAndEqualTo(Rejected));
        assertThat(operation.description, isPresentAndEqualTo("Insufficient funds on account '" + accountId + "'"));
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance, initialAccount.version)));
    }
}