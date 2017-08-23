package mtymes.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.listeners.MockCreationListener;
import org.mockito.quality.Strictness;

import java.util.List;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoMoreInteractions;

// todo: turn this into a rule somehow
// todo: move into test-infrastructure
public abstract class StrictMockTest {

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    private static final List<Object> mocks = newCopyOnWriteArrayList();
    private static final MockCreationListener mockCreationListener = (mock, settings) -> mocks.add(mock);

    @BeforeClass
    public static void setUpMockitoListener() throws Exception {
        Mockito.framework().addListener(mockCreationListener);
    }

    @After
    public void verifyMocksStrictly() throws Exception {
        if (mocks.size() > 0) {
            try {
                verifyNoMoreInteractions(mocks.toArray());
            } finally {
                clearInvocations(mocks.toArray());
            }
        }
    }

    @AfterClass
    public static void releaseMockitoListener() throws Exception {
        Mockito.framework().removeListener(mockCreationListener);
    }
}
