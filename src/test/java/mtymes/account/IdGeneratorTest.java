package mtymes.account;

import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import org.junit.Test;

import java.util.Set;

import static javafixes.common.CollectionUtil.newSet;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class IdGeneratorTest {

    private IdGenerator idGenerator = new IdGenerator();

    @Test
    public void shouldGenerateNewOperationIdEachTime() {
        Set<OperationId> generateOperationIds = newSet();

        for (int i = 0; i < 100; i++) {
            OperationId operationId = idGenerator.nextOperationId();

            assertThat(operationId, notNullValue());
            assertThat(generateOperationIds.contains(operationId), is(false));

            generateOperationIds.add(operationId);
        }
    }

    @Test
    public void shouldGenerateNewAccountIdEachTime() {
        Set<AccountId> generateAccountIds = newSet();

        for (int i = 0; i < 100; i++) {
            AccountId accountId = idGenerator.nextAccountId();

            assertThat(accountId, notNullValue());
            assertThat(generateAccountIds.contains(accountId), is(false));

            generateAccountIds.add(accountId);
        }
    }
}