package mtymes.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.FinalState;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferFrom;
import org.junit.Test;

import java.util.Optional;

import static mtymes.test.Random.*;

public class JsonUtilTest {

    @Test
    public void shouldConvertOperation() throws JsonProcessingException {
        AccountId fromAccountId = randomAccountId();
        LoggedOperation operation = new LoggedOperation(
                randomOpLogId(fromAccountId),
                new TransferFrom(new TransferDetail(
                        randomTransferId(),
                        fromAccountId,
                        randomAccountId(),
                        randomPositiveAmount()
                )),
                Optional.of(FinalState.Applied),
                Optional.empty()
        );

        // When
        String jsonValue = JsonUtil.toJsonString(operation);

        // Then
        // todo: implement
        System.out.println(jsonValue);
//        assertEquals(
//
//        );
    }
}