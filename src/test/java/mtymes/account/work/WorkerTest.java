package mtymes.account.work;

import mtymes.account.dao.OperationDao;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.handler.OperationDispatcher;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static javafixes.common.CollectionUtil.newList;
import static mtymes.test.Condition.otherThan;
import static mtymes.test.Random.randomAccountId;
import static mtymes.test.Random.randomOpLogId;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class WorkerTest extends StrictMockTest {

    private OperationDao operationDao;
    private OperationDispatcher dispatcher;

    private Worker worker;

    @Before
    public void setUp() throws Exception {
        operationDao = mock(OperationDao.class);
        dispatcher = mock(OperationDispatcher.class);

        worker = new Worker(operationDao, dispatcher);
    }


    @Test
    public void shouldDoNothingIfThereAreNoUnfinishedOperations() {
        AccountId accountId = randomAccountId();
        given(operationDao.findUnfinishedOperationLogIds(accountId)).willReturn(emptyList());

        // When
        worker.runUnfinishedOperations(accountId);

        // Then
        // do nothing
    }

    @Test
    public void shouldRunAllUnfinishedOperations() {
        AccountId accountId = randomAccountId();
        OpLogId opLogId1 = randomOpLogId(accountId);
        OpLogId opLogId2 = randomOpLogId(accountId, otherThan(opLogId1));
        OpLogId opLogId3 = randomOpLogId(accountId, otherThan(opLogId1, opLogId2));
        LoggedOperation operation1 = mock(LoggedOperation.class);
        LoggedOperation operation2 = mock(LoggedOperation.class);
        LoggedOperation operation3 = mock(LoggedOperation.class);

        when(operationDao.findUnfinishedOperationLogIds(accountId)).thenReturn(newList(opLogId1, opLogId2, opLogId3));
        doReturn(Optional.of(operation1)).when(operationDao).findLoggedOperation(opLogId1);
        doReturn(Optional.of(operation2)).when(operationDao).findLoggedOperation(opLogId2);
        doReturn(Optional.of(operation3)).when(operationDao).findLoggedOperation(opLogId3);

        // Then
        doNothing().when(dispatcher).dispatchOperation(operation1);
        doNothing().when(dispatcher).dispatchOperation(operation2);
        doNothing().when(dispatcher).dispatchOperation(operation3);

        // When
        worker.runUnfinishedOperations(accountId);
    }

    @Test
    public void shouldNotContinueToNextOperationOnFailure() {
        AccountId accountId = randomAccountId();
        OpLogId opLogId1 = randomOpLogId(accountId);
        OpLogId opLogId2 = randomOpLogId(accountId, otherThan(opLogId1));
        OpLogId opLogId3 = randomOpLogId(accountId, otherThan(opLogId1, opLogId2));
        LoggedOperation operation1 = mock(LoggedOperation.class);
        LoggedOperation operation2 = mock(LoggedOperation.class);
        LoggedOperation operation3 = mock(LoggedOperation.class);

        when(operationDao.findUnfinishedOperationLogIds(accountId)).thenReturn(newList(opLogId1, opLogId2, opLogId3));
        doReturn(Optional.of(operation1)).when(operationDao).findLoggedOperation(opLogId1);
        doReturn(Optional.of(operation2)).when(operationDao).findLoggedOperation(opLogId2);

        // Then
        doNothing().when(dispatcher).dispatchOperation(operation1);
        RuntimeException expectedException = new RuntimeException("some exception");
        doThrow(expectedException).when(dispatcher).dispatchOperation(operation2);

        try {
            // When
            worker.runUnfinishedOperations(accountId);

            fail("the call should fail");
        } catch (RuntimeException actualException) {
            assertThat(actualException.getMessage(), equalTo(expectedException.getMessage()));
        }
    }
}