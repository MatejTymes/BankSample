package mtymes.account.dao;

import javafixes.object.Tuple;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;

import java.util.List;

public interface OpLogDao {

    SeqId registerOperationId(AccountId accountId, OperationId operationId);

    void markAsFinished(OperationId operationId);

    List<Tuple<OperationId, SeqId>> findUnfinishedOperationIds(AccountId accountId);
}
