package mtymes.account.dao.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.account.Version;
import org.bson.Document;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class MongoAccountDao extends MongoBaseDao implements AccountDao {

    public static final String ACCOUNT_ID = "accountId";
    public static final String BALANCE = "balance";
    public static final String VERSION = "version";

    private final MongoCollection<Document> accounts;
    private final MongoMapper mapper = new MongoMapper();

    public MongoAccountDao(MongoCollection<Document> accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean createAccount(AccountId accountId, Version version) {
        try {
            accounts.insertOne(docBuilder()
                    .put(ACCOUNT_ID, accountId)
                    .put(BALANCE, Decimal.ZERO)
                    .put(VERSION, version)
                    .build());
            return true;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    @Override
    public boolean updateBalance(AccountId accountId, Decimal newBalance, Version oldVersion, Version newVersion) {
        checkArgument(oldVersion.isBefore(newVersion), "oldVersion must be before newVersion");

        try {
            UpdateResult result = accounts.updateOne(
                    docBuilder()
                            .put(ACCOUNT_ID, accountId)
                            .put(VERSION, oldVersion)
                            .build(),
                    doc("$set", docBuilder()
                            .put(BALANCE, newBalance)
                            .put(VERSION, newVersion)
                            .build())
            );
            return result.getModifiedCount() == 1;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    @Override
    public Optional<Account> findAccount(AccountId accountId) {
        return findOne(
                accounts,
                doc(ACCOUNT_ID, accountId),
                doc -> new Account(
                        mapper.getAccountId(doc, ACCOUNT_ID),
                        mapper.getDecimal(doc, BALANCE),
                        mapper.getVersion(doc, VERSION)
                )
        );
    }

    @Override
    public Optional<Version> findCurrentVersion(AccountId accountId) {
        return findOne(
                accounts,
                doc(ACCOUNT_ID, accountId),
                doc(VERSION, 1),
                doc -> mapper.getVersion(doc, VERSION)
        );
    }
}
