package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static org.junit.Assert.assertThat;

public class CreateAccountHandlerDisasterRecoveryTest extends BaseOperationHandlerDisasterRecoveryTest {

    private CreateAccountHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new CreateAccountHandler(brokenAccountDao, brokenOperationDao);
    }

    @Test
    public void shouldSucceedToCreateAccountEvenIfAnyDbCallFails() {
        AccountId accountId = newAccountId();
        CreateAccount createAccount = new CreateAccount(accountId);
        OpLogId opLogId = operationDao.storeOperation(createAccount);

        // When
        retryWhileSystemIsBroken(
                () -> handler.handleOperation(opLogId, createAccount)
        );

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Applied));
        assertThat(operation.description, isNotPresent());
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, Decimal.ZERO, opLogId.seqId)));
    }

    @Test
    public void shouldFailToCreateAccountIfItIsAlreadyPresentEvenIfAnyDbCallFails() {
        AccountId accountId = newAccountId();
        Account initialAccount = createAccount(accountId);
        CreateAccount createAccount = new CreateAccount(accountId);
        OpLogId opLogId = operationDao.storeOperation(createAccount);

        // When
        retryWhileSystemIsBroken(
                () -> handler.handleOperation(opLogId, createAccount)
        );

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Rejected));
        assertThat(operation.description, isPresentAndEqualTo("Account '" + accountId + "' already exists"));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(initialAccount));
    }
}