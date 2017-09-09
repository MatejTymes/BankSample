package mtymes.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import mtymes.account.domain.account.AccountId;
import org.asynchttpclient.Response;
import org.json.JSONException;

import static mtymes.common.json.JsonUtil.toJsonObject;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class ResponseWrapper {

    private final Response response;

    public ResponseWrapper(Response response) {
        this.response = response;
    }

    public ResponseWrapper shouldHaveStatus(int status) {
        assertThat(response.getStatusCode(), equalTo(status));
        return this;
    }

    public ResponseWrapper shouldHaveBody(ObjectNode jsonBody) {
        assertThat(response.getHeader("Content-Type"), equalTo("application/json"));
        try {
            assertEquals(jsonBody.toString(), response.getResponseBody(), true);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public AccountId accountId() {
        return AccountId.accountId(toJsonObject(response.getResponseBody()).get("accountId").asText());
    }
}
