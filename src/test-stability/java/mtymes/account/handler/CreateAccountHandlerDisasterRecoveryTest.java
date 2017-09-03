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
import static mtymes.account.domain.operation.FinalState.Success;
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
    public void shouldSucceedToCreateAccountEvenIfDbAccessFailedOnEachStep() {
        AccountId accountId = newAccountId();
        CreateAccount createAccount = new CreateAccount(accountId);
        OpLogId opLogId = operationDao.storeOperation(createAccount);

        // When
        repeatWhileSystemIsBroken(
                () -> handler.handleOperation(opLogId, createAccount)
        );

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Success));
        assertThat(operation.description, isNotPresent());
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, Decimal.ZERO, opLogId.version)));
    }

}