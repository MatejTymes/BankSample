package mtymes.api;

import javafixes.math.Decimal;
import mtymes.account.domain.account.AccountId;

public class BankApi extends BaseApi {

    public BankApi(String hostName, int port) {
        super(hostName, port);
    }

    public ResponseWrapper loadAccount(AccountId accountId) {
        return get(path("account").path(accountId.toString()));
    }

    public ResponseWrapper createAccount() {
        return post(path("account/new"));
    }

    public ResponseWrapper depositMoney(AccountId accountId, Decimal amount) {
        return post(path("account").path(accountId.toString()).path("deposit").path(amount.toPlainString()));
    }

    public ResponseWrapper withdrawMoney(AccountId accountId, Decimal amount) {
        return post(path("account").path(accountId.toString()).path("withdraw").path(amount.toPlainString()));
    }
}
