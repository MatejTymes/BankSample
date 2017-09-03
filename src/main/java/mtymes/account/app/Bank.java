package mtymes.account.app;

import mtymes.account.OperationExecutor;
import mtymes.account.config.Dependencies;
import mtymes.account.config.SystemProperties;
import mtymes.common.json.JsonUtil;
import spark.ResponseTransformer;

import static spark.Spark.*;

// todo: test
// todo: add ability to shut it down
public class Bank {

    private static final ResponseTransformer jsonTransformer = JsonUtil::toJson;

    private final SystemProperties properties;

    public Bank(SystemProperties properties) {
        this.properties = properties;
    }

    public void start() {
        // todo: finish
        port(properties.appPort());

        OperationExecutor executor = new Dependencies(properties).executor;

        post(
                "/createNewAccount",
                (req, res) -> executor.createAccount(),
                jsonTransformer
        );
        after((req, res) -> {
            res.type("application/json");
        });
    }
}
