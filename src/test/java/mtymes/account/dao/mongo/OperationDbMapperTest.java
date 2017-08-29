package mtymes.account.dao.mongo;

import mtymes.account.domain.operation.*;
import org.bson.Document;
import org.junit.Test;

import java.util.List;

import static javafixes.common.CollectionUtil.newList;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class OperationDbMapperTest {

    private OperationDbMapper dbMapper = new OperationDbMapper();

    @Test
    public void shouldBeAbleToConvertOperationToDocumentAndBackAgain() {
        List<Operation> allOperations = newList(
                new CreateAccount(randomAccountId()),
                new DepositMoney(randomAccountId(), randomPositiveDecimal()),
                new WithdrawMoney(randomAccountId(), randomPositiveDecimal()),
                new TransferMoneyFrom(new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveDecimal())),
                new TransferMoneyTo(new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveDecimal()))
        );

        for (Operation originalOperation : allOperations) {
            // When & Then
            Document document = originalOperation.apply(dbMapper);
            Operation reconstructedOperation = dbMapper.toOperation(originalOperation.getClass().getSimpleName(), document);
            assertThat(reconstructedOperation, equalTo(originalOperation));
        }
    }
}