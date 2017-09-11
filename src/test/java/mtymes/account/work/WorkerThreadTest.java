package mtymes.account.work;

import mtymes.account.domain.account.AccountId;
import mtymes.common.util.SetQueue;
import mtymes.common.util.Sleeper;
import mtymes.test.StrictMockTest;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;

import static mtymes.test.Random.randomAccountId;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class WorkerThreadTest extends StrictMockTest {

    private SetQueue queue;
    private Worker worker;
    private Sleeper sleeper;
    private Duration timeoutIfNoWork = Duration.ofMillis(125);

    private WorkerThread thread;

    @Before
    public void setUp() throws Exception {
        queue = mock(SetQueue.class);
        worker = mock(Worker.class);
        sleeper = mock(Sleeper.class);

        thread = new WorkerThread(queue, worker, sleeper, timeoutIfNoWork);
    }

    @Test
    public void shouldDoNothingIfNoWorkIsPresent() throws Exception {
        when(queue.takeNextAvailable()).thenReturn(Optional.empty());

        InterruptedException expectedException = new InterruptedException("thread shutdown");
        doThrow(expectedException).when(sleeper).sleepFor(timeoutIfNoWork);

        try {
            // When
            thread.run();
        } catch (InterruptedException actualException) {
            assertThat(actualException.getMessage(), equalTo(expectedException.getMessage()));
        }
        assertThat(thread.isWorking(), is(false));
    }

    @Test
    public void shouldSubmitWorkToWorkerIfExists() throws Exception {
        AccountId accountId = randomAccountId();
        when(queue.takeNextAvailable()).thenReturn(Optional.of(accountId));
        doAnswer(inv -> {
            assertThat(thread.isWorking(), is(true));
            return null;
        }).when(worker).runUnfinishedOperations(accountId);

        InterruptedException expectedException = new InterruptedException("thread shutdown");
        doThrow(expectedException).when(sleeper).sleepFor(0L);

        try {
            // When
            thread.run();
        } catch (InterruptedException actualException) {
            assertThat(actualException.getMessage(), equalTo(expectedException.getMessage()));
        }
        assertThat(thread.isWorking(), is(false));
    }

    @Test
    public void shouldPutWorkBackToQueueIfWorkerFails() throws Exception {
        AccountId accountId = randomAccountId();
        when(queue.takeNextAvailable()).thenReturn(Optional.of(accountId));
        doThrow(new RuntimeException("worker failed")).when(worker).runUnfinishedOperations(accountId);
        doNothing().when(queue).add(accountId);

        InterruptedException expectedException = new InterruptedException("thread shutdown");
        doThrow(expectedException).when(sleeper).sleepFor(0L);

        try {
            // When
            thread.run();
        } catch (InterruptedException actualException) {
            assertThat(actualException.getMessage(), equalTo(expectedException.getMessage()));
        }
        assertThat(thread.isWorking(), is(false));
    }

    @Test
    public void shouldHandleWorkRepeatedlyUntilInterrupted() throws Exception {
        AccountId accountId1 = randomAccountId();
        AccountId accountId2 = randomAccountId();

        when(queue.takeNextAvailable()).thenReturn(Optional.of(accountId1));

        when(queue.takeNextAvailable()).thenReturn(Optional.of(accountId2));

        when(queue.takeNextAvailable()).thenReturn(Optional.empty());
        InterruptedException expectedException = new InterruptedException("thread shutdown");
        doThrow(expectedException).when(sleeper).sleepFor(timeoutIfNoWork);

        try {
            // When
            thread.run();
        } catch (InterruptedException actualException) {
            assertThat(actualException.getMessage(), equalTo(expectedException.getMessage()));
        }
        assertThat(thread.isWorking(), is(false));
    }
}