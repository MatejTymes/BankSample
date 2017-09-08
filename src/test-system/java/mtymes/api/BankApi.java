package mtymes.api;

import org.asynchttpclient.Response;

// todo: wrap Response into more functional ResponseWrapper
public class BankApi extends BaseApi {

    public BankApi(String hostName, int port) {
        super(hostName, port);
    }

    public Response createAccount() {
        return post(path("account/new"));
    }

    public Response loadAccount(String accountId) {
        return get(path("account").path(accountId));
    }
}
