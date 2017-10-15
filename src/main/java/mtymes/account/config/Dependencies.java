package mtymes.account.config;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import mtymes.account.IdGenerator;
import mtymes.account.OperationSubmitter;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OpLogDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.dao.mongo.MongoAccountDao;
import mtymes.account.dao.mongo.MongoOpLogDao;
import mtymes.account.dao.mongo.MongoOperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.handler.*;
import mtymes.account.work.Sweatshop;
import mtymes.account.work.Worker;
import mtymes.common.util.SetQueue;

import static mtymes.account.dao.mongo.MongoCollections.*;

public class Dependencies {

    public final AccountDao accountDao;
    public final OperationSubmitter submitter;
    public final Sweatshop sweatshop;

    public Dependencies(SystemProperties properties) {
        MongoClient mongoClient = new MongoClient(properties.dbHostName(), properties.dbPort());
        MongoDatabase database = mongoClient.getDatabase(properties.dbName());
        this.accountDao = new MongoAccountDao(accountsCollection(database));
        OpLogDao opLogDao = new MongoOpLogDao(opLogCollection(database));
        OperationDao operationDao = new MongoOperationDao(operationsCollection(database));

        IdGenerator idGenerator = new IdGenerator();
        SetQueue<AccountId> workQueue = new SetQueue<>();

        OperationDispatcher dispatcher = new OperationDispatcher(
                opLogDao,
                new CreateAccountHandler(accountDao, operationDao),
                new DepositToHandler(accountDao, operationDao),
                new WithdrawFromHandler(accountDao, operationDao),
                new TransferFromHandler(accountDao, operationDao, opLogDao, workQueue),
                new TransferToHandler(accountDao, operationDao)
        );
        Worker worker = new Worker(opLogDao, operationDao, dispatcher);

        this.sweatshop = new Sweatshop(workQueue, properties.backgroundWorkerCount(), worker, properties.workerIdleTimeout()).start();
        this.submitter = new OperationSubmitter(idGenerator, accountDao, operationDao, opLogDao, worker);
    }

    public void shutdown() {
        sweatshop.shutdown();
    }
}
