package mtymes.account.work;

import mtymes.account.dao.OpLogDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.handler.OperationDispatcher;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static javafixes.common.CollectionUtil.newList;
import static javafixes.object.Tuple.tuple;
import static mtymes.test.Condition.otherThan;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class WorkerTest extends StrictMockTest {

    private OpLogDao opLogDao;
    private OperationDao operationDao;
    private OperationDispatcher dispatcher;

    private Worker worker;

    @Before
    public void setUp() throws Exception {
        opLogDao = mock(OpLogDao.class);
        operationDao = mock(OperationDao.class);
        dispatcher = mock(OperationDispatcher.class);

        worker = new Worker(opLogDao, operationDao, dispatcher);
    }


    @Test
    public void shouldDoNothingIfThereAreNoUnfinishedOperations() {
        AccountId accountId = randomAccountId();
        given(opLogDao.findUnfinishedOperationIds(accountId)).willReturn(emptyList());

        // When
        worker.runUnfinishedOperations(accountId);

        // Then
        // do nothing
    }

    @Test
    public void shouldRunAllUnfinishedOperations() {
        AccountId accountId = randomAccountId();
        SeqId seqId1 = randomSeqId();
        SeqId seqId2 = randomSeqId(otherThan(seqId1));
        SeqId seqId3 = randomSeqId(otherThan(seqId1, seqId2));
        OperationId operationId1 = randomOperationId();
        OperationId operationId2 = randomOperationId();
        OperationId operationId3 = randomOperationId();

        LoggedOperation operation1 = mock(LoggedOperation.class);
        LoggedOperation operation2 = mock(LoggedOperation.class);
        LoggedOperation operation3 = mock(LoggedOperation.class);

        when(opLogDao.findUnfinishedOperationIds(accountId)).thenReturn(newList(
                tuple(operationId1, seqId1),
                tuple(operationId2, seqId2),
                tuple(operationId3, seqId3)
        ));
        doReturn(Optional.of(operation1)).when(operationDao).findLoggedOperation(operationId1);
        doReturn(Optional.of(operation2)).when(operationDao).findLoggedOperation(operationId2);
        doReturn(Optional.of(operation3)).when(operationDao).findLoggedOperation(operationId3);

        // Then
        doNothing().when(dispatcher).dispatchOperation(seqId1, operation1);
        doNothing().when(dispatcher).dispatchOperation(seqId2, operation2);
        doNothing().when(dispatcher).dispatchOperation(seqId3, operation3);

        // When
        worker.runUnfinishedOperations(accountId);
    }

    @Test
    public void shouldNotContinueToNextOperationOnFailure() {
        AccountId accountId = randomAccountId();
        SeqId seqId1 = randomSeqId();
        SeqId seqId2 = randomSeqId(otherThan(seqId1));
        SeqId seqId3 = randomSeqId(otherThan(seqId1, seqId2));
        OperationId operationId1 = randomOperationId();
        OperationId operationId2 = randomOperationId();
        OperationId operationId3 = randomOperationId();

        LoggedOperation operation1 = mock(LoggedOperation.class);
        LoggedOperation operation2 = mock(LoggedOperation.class);

        when(opLogDao.findUnfinishedOperationIds(accountId)).thenReturn(newList(
                tuple(operationId1, seqId1),
                tuple(operationId2, seqId2),
                tuple(operationId3, seqId3)
        ));
        doReturn(Optional.of(operation1)).when(operationDao).findLoggedOperation(operationId1);
        doReturn(Optional.of(operation2)).when(operationDao).findLoggedOperation(operationId2);

        // Then
        doNothing().when(dispatcher).dispatchOperation(seqId1, operation1);
        RuntimeException expectedException = new RuntimeException("some exception");
        doThrow(expectedException).when(dispatcher).dispatchOperation(seqId2, operation2);

        try {
            // When
            worker.runUnfinishedOperations(accountId);

            fail("the call should fail");
        } catch (RuntimeException actualException) {
            assertThat(actualException.getMessage(), equalTo(expectedException.getMessage()));
        }
    }
}