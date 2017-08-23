package mtymes.account.dao.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.util.Optional;
import java.util.UUID;

import static javafixes.math.Decimal.d;
import static mtymes.account.domain.account.AccountId.accountId;
import static mtymes.account.domain.operation.SeqId.seqId;
import static mtymes.common.mongo.DocumentBuilder.doc;
import static mtymes.common.mongo.DocumentBuilder.docBuilder;

public class MongoAccountDao extends MongoBaseDao implements AccountDao {

    public static final String ACCOUNT_ID = "accountId";
    public static final String BALANCE = "balance";
    public static final String LAST_APPLIED_OP_SEQ_ID = "appliedOpSeqId";

    private final MongoCollection<Document> accounts;

    public MongoAccountDao(MongoCollection<Document> accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean createAccount(AccountId accountId, SeqId seqId) {
        try {
            accounts.insertOne(docBuilder()
                    .put(ACCOUNT_ID, accountId)
                    .put(BALANCE, Decimal.ZERO)
                    .put(LAST_APPLIED_OP_SEQ_ID, seqId)
                    .build());
            return true;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    @Override
    public boolean updateBalance(AccountId accountId, Decimal newBalance, SeqId fromSeqId, SeqId toSeqId) {
        try {
            UpdateResult result = accounts.updateOne(
                    docBuilder()
                            .put(ACCOUNT_ID, accountId)
                            .put(LAST_APPLIED_OP_SEQ_ID, fromSeqId)
                            .build(),
                    doc("$set", docBuilder()
                            .put(BALANCE, newBalance)
                            .put(LAST_APPLIED_OP_SEQ_ID, toSeqId)
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
                        accountId(UUID.fromString(doc.getString(ACCOUNT_ID))),
                        d(((Decimal128)doc.get(BALANCE)).bigDecimalValue()),
                        seqId(doc.getLong(LAST_APPLIED_OP_SEQ_ID))
                )
        );
    }

    // todo: test
    @Override
    public Optional<SeqId> findLastAppliedOpSeqId(AccountId accountId) {
        return findOne(
                accounts,
                doc(ACCOUNT_ID, accountId),
                doc -> seqId(doc.getLong(LAST_APPLIED_OP_SEQ_ID))
        );
    }
}
