package mtymes.account.app;

import mtymes.account.OperationSubmitter;
import mtymes.account.config.Dependencies;
import mtymes.account.config.SystemProperties;
import mtymes.account.dao.AccountDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.work.Sweatshop;
import mtymes.common.domain.Failure;
import mtymes.common.json.JsonUtil;
import spark.ResponseTransformer;
import spark.Service;

import java.util.Optional;

import static java.lang.String.format;
import static javafixes.math.Decimal.decimal;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.common.domain.Failure.failure;
import static mtymes.common.json.JsonUtil.toJsonString;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

public class Bank {

    private static final ResponseTransformer jsonTransformer = JsonUtil::toJsonString;

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
        Sweatshop sweatshop = dependencies.sweatshop;

        Service spark = Service.ignite();
        spark.port(port);

        spark.get("/account/:accountId", (req, res) -> {
            AccountId accountId = accountId(req.params(":accountId"));
            Optional<Account> account = accountDao.findAccount(accountId);
            if (account.isPresent()) {
                return account.get();
            } else {
                res.status(400);
                return failure(format("Account '%s' not found", accountId));
            }
        }, jsonTransformer);

        spark.post("/account/new", (req, res) -> submitter
                        .createAccount()
                        .handle(
                                failure -> res.status(500),
                                account -> res.status(201)
                        )
                        .value(),
                jsonTransformer);

        spark.post("/account/:accountId/deposit/:amount", (req, res) -> submitter
                        .depositMoney(
                                accountId(req.params(":accountId")),
                                decimal(req.params(":amount")))
                        .handle(
                                failure -> res.status(400),
                                success -> res.status(200)
                        )
                        .value(),
                jsonTransformer);

        spark.post("/account/:accountId/withdraw/:amount", (req, res) -> submitter
                        .withdrawMoney(
                                accountId(req.params(":accountId")),
                                decimal(req.params(":amount")))
                        .handle(
                                failure -> res.status(400),
                                success -> res.status(200)
                        )
                        .value(),
                jsonTransformer);

        spark.post("/account/:fromAccountId/transfer/:amount/to/:toAccountId", (req, res) -> submitter
                        .transferMoney(
                                accountId(req.params(":fromAccountId")),
                                accountId(req.params(":toAccountId")),
                                decimal(req.params(":amount"))
                        )
                        .handle(
                                failure -> res.status(400),
                                success -> res.status(200)
                        )
                        .value(),
                jsonTransformer);

        spark.get("/work/queued/stats", (req, res) -> sweatshop.queuedWorkStats(), jsonTransformer);

        spark.after((req, res) -> res.type("application/json"));

        spark.exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.type("application/json");
            String message = e.getMessage();
            Failure failure = (message != null)
                    ? failure(message)
                    : failure(getStackTrace(e));
            res.body(toJsonString(failure));
        });

        return this;
    }

    public void shutdown() {
        this.dependencies.shutdown();
    }

    public int getPort() {
        return port;
    }
}
