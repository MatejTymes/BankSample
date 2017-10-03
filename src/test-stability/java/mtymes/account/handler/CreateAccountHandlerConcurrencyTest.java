package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.test.ConcurrencyUtil.runConcurrentlyOnNThreads;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.randomAccountId;
import static mtymes.test.Random.randomOperationId;
import static org.junit.Assert.assertThat;

public class CreateAccountHandlerConcurrencyTest extends BaseOperationHandlerStabilityTest {

    private CreateAccountHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new CreateAccountHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToCreateAccountOnConcurrentExecution() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        CreateAccount createAccount = new CreateAccount(operationId, accountId);
        operationDao.storeOperation(createAccount);
        SeqId seqId = opLogDao.registerOperationId(accountId, operationId);

        // When
        runConcurrentlyOnNThreads(
                () -> handler.handleOperation(seqId, createAccount),
                50
        );

        // Then
        LoggedOperation operation = loadOperation(operationId);
        assertThat(operation.finalState, isPresentAndEqualTo(Applied));
        assertThat(operation.description, isNotPresent());
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, Decimal.ZERO, seqId)));
    }

    @Test
    public void shouldFailToCreateAccountIfItIsAlreadyPresentOnConcurrentExecution() {
        OperationId operationId = randomOperationId();
        AccountId accountId = randomAccountId();
        Account initialAccount = createAccount(accountId);
        CreateAccount createAccount = new CreateAccount(operationId, accountId);
        operationDao.storeOperation(createAccount);
        SeqId seqId = opLogDao.registerOperationId(accountId, operationId);

        // When
        runConcurrentlyOnNThreads(
                () -> handler.handleOperation(seqId, createAccount),
                50
        );

        // Then
        LoggedOperation operation = loadOperation(operationId);
        assertThat(operation.finalState, isPresentAndEqualTo(Rejected));
        assertThat(operation.description, isPresentAndEqualTo("Account '" + accountId + "' already exists"));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(initialAccount));
    }
}