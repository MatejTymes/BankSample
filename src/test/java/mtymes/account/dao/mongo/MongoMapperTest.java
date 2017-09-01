package mtymes.account.dao.mongo;

import mtymes.account.domain.operation.*;
import org.bson.Document;
import org.junit.Test;

import java.util.List;

import static javafixes.common.CollectionUtil.newList;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class MongoMapperTest {

    private MongoMapper dbMapper = new MongoMapper();

    @Test
    public void shouldBeAbleToConvertOperationToDocumentAndBackAgain() {
        List<Operation> allOperations = newList(
                new CreateAccount(randomAccountId()),
                new DepositTo(randomAccountId(), randomPositiveDecimal()),
                new WithdrawFrom(randomAccountId(), randomPositiveDecimal()),
                new TransferFrom(new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveDecimal())),
                new TransferTo(new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveDecimal()))
        );

        for (Operation originalOperation : allOperations) {
            // When & Then
            Document document = originalOperation.apply(dbMapper);
            Operation reconstructedOperation = dbMapper.toOperation(originalOperation.getClass().getSimpleName(), document);
            assertThat(reconstructedOperation, equalTo(originalOperation));
        }
    }
}