package mtymes.account;

import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.TransferId;
import org.junit.Test;

import java.util.Set;

import static javafixes.common.CollectionUtil.newSet;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class IdGeneratorTest {

    private IdGenerator idGenerator = new IdGenerator();

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

    @Test
    public void shouldGenerateNewTransferIdEachTime() {
        Set<TransferId> generateTransferIds = newSet();

        for (int i = 0; i < 100; i++) {
            TransferId transferId = idGenerator.nextTransferId();

            assertThat(transferId, notNullValue());
            assertThat(generateTransferIds.contains(transferId), is(false));

            generateTransferIds.add(transferId);
        }
    }
}