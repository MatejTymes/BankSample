package mtymes.account.app;

import mtymes.account.OperationSubmitter;
import mtymes.account.config.Dependencies;
import mtymes.account.config.SystemProperties;
import mtymes.account.dao.AccountDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.common.json.JsonUtil;
import spark.ResponseTransformer;

import java.util.Optional;

import static java.lang.String.format;
import static mtymes.account.domain.Failure.failure;
import static mtymes.account.domain.account.AccountId.accountId;
import static spark.Spark.*;

// todo: test
public class Bank {

    private static final ResponseTransformer jsonTransformer = JsonUtil::toJson;

    private final SystemProperties properties;
    private final int port;

    public volatile Dependencies dependencies;

    public Bank(SystemProperties properties) {
        this.properties = properties;
        this.port = properties.appPort();
    }

    public Bank start() {
        this.dependencies = new Dependencies(properties);
        AccountDao accountDao = dependencies.accountDao;
        OperationSubmitter submitter = dependencies.submitter;

        port(port);

        get("/account/:accountId", (req, res) -> {
            String accountIdParam = req.params(":accountId");
            AccountId accountId = accountId(accountIdParam);
            Optional<Account> account = accountDao.findAccount(accountId);
            if (account.isPresent()) {
                return account.get();
            } else {
                return failure(format("Account '%s' not found", accountIdParam));
            }
        }, jsonTransformer);

        post("/account/new", (req, res) -> submitter.createAccount().get(), jsonTransformer);
//        post("/account/:accountId/deposit/:amount")

        after((req, res) -> res.type("application/json"));

        return this;
    }

    public void shutdown() {
        this.dependencies.shutdown();
    }

    public int getPort() {
        return port;
    }
}
