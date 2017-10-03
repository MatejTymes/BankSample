package mtymes.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static javafixes.common.CollectionUtil.newList;
import static mtymes.common.json.JsonBuilder.jsonBuilder;
import static mtymes.test.Random.randomAccountId;
import static mtymes.test.Random.randomAmount;

public class JsonUtilTest {

    @Test
    public void shouldConvertObjectToString() throws Exception {
        AccountId accountId = randomAccountId();
        Decimal decimal = randomAmount();
        String string = "some text";
        for (Optional<String> optionalString : newList(Optional.<String>empty(), Optional.of("some other text"))) {
            String jsonString = JsonUtil.toJsonString(new JsonTestObject(
                    accountId,
                    decimal,
                    optionalString,
                    string
            ));

            JsonBuilder expectedJson = jsonBuilder()
                    .with("accountIdValue", accountId.toString())
                    .with("decimalValue", decimal)
                    .with("stringValue", string);
            if (optionalString.isPresent()) {
                expectedJson = expectedJson.with("optionalStringValue", optionalString.get());
            }
            JSONAssert.assertEquals(expectedJson.buildString(), jsonString, true);
        }
    }


    private class JsonTestObject {

        public final AccountId accountIdValue;
        public final Decimal decimalValue;
        @JsonInclude(value= NON_ABSENT, content= NON_EMPTY)
        public final Optional<String> optionalStringValue;
        public final String stringValue;

        private JsonTestObject(AccountId accountIdValue, Decimal decimalValue, Optional<String> optionalStringValue, String stringValue) {
            this.accountIdValue = accountIdValue;
            this.decimalValue = decimalValue;
            this.optionalStringValue = optionalStringValue;
            this.stringValue = stringValue;
        }
    }
}