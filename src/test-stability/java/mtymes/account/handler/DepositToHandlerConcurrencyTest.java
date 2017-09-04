package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositTo;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import org.junit.Before;
import org.junit.Test;

import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.test.ConcurrencyUtil.runConcurrentlyOnNThreads;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class DepositToHandlerConcurrencyTest extends BaseOperationHandlerStabilityTest {

    private DepositToHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new DepositToHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToDepositToOnConcurrentExecution() {
        Decimal amount = randomPositiveAmount();

        Decimal initialBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        AccountId accountId = createAccountWithInitialBalance(initialBalance).accountId;

        DepositTo depositTo = new DepositTo(accountId, amount);
        OpLogId opLogId = operationDao.storeOperation(depositTo);

        // When
        runConcurrentlyOnNThreads(
                () -> handler.handleOperation(opLogId, depositTo),
                50
        );

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Applied));
        assertThat(operation.description, isNotPresent());
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance.plus(amount), opLogId.seqId)));
    }
}