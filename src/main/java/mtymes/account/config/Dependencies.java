package mtymes.account.config;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import javafixes.concurrency.Runner;
import mtymes.account.OperationExecutor;
import mtymes.account.WorkQueue;
import mtymes.account.Worker;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.dao.mongo.MongoAccountDao;
import mtymes.account.dao.mongo.MongoOperationDao;
import mtymes.account.handler.*;

import static javafixes.concurrency.Runner.runner;
import static mtymes.account.dao.mongo.Collections.accountsCollection;
import static mtymes.account.dao.mongo.Collections.operationsCollection;

// todo: add ability to shutdown
public class Dependencies {

    public final OperationExecutor executor;

    public Dependencies(SystemProperties properties) {
        MongoClient mongoClient = new MongoClient(properties.dbHostName(), properties.dbPort());
        MongoDatabase database = mongoClient.getDatabase(properties.dbName());
        AccountDao accountDao = new MongoAccountDao(accountsCollection(database));
        OperationDao operationDao = new MongoOperationDao(operationsCollection(database));

        WorkQueue workQueue = new WorkQueue();
        HandlerDispatcher dispatcher = new HandlerDispatcher(
                new CreateAccountHandler(accountDao, operationDao),
                new DepositToHandler(accountDao, operationDao),
                new WithdrawFromHandler(accountDao, operationDao),
                new TransferFromHandler(accountDao, operationDao, workQueue),
                new TransferToHandler(accountDao, operationDao)
        );

        Runner runner = runner(properties.backgroundWorkerCount());
        for (int i = 0; i < properties.backgroundWorkerCount(); i++) {
            runner.runTask(new Worker(workQueue, operationDao, dispatcher, properties.workerIdleTimeout()));
        }

        this.executor = new OperationExecutor(operationDao, dispatcher);
    }
}
