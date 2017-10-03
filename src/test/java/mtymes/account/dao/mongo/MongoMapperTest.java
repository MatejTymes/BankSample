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
        OperationId operationId = randomOperationId();
        List<Operation> allOperations = newList(
                new CreateAccount(operationId, randomAccountId()),
                new DepositTo(operationId, randomAccountId(), randomPositiveAmount()),
                new WithdrawFrom(operationId, randomAccountId(), randomPositiveAmount()),
                new TransferFrom(operationId, operationId, new TransferDetail(randomAccountId(), randomAccountId(), randomPositiveAmount())),
                new TransferTo(operationId, new TransferDetail(randomAccountId(), randomAccountId(), randomPositiveAmount()))
        );

        for (Operation originalOperation : allOperations) {
            // When & Then
            Document document = originalOperation.apply(dbMapper);
            Operation reconstructedOperation = dbMapper.toOperation(operationId, originalOperation.getClass().getSimpleName(), document);
            assertThat(reconstructedOperation, equalTo(originalOperation));
        }
    }
}