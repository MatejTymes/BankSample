package mtymes.account.domain.operation;

import mtymes.test.StrictMockTest;
import org.junit.Test;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransferFromTest extends StrictMockTest {

    @Test
    public void shouldCreateOperation() {
        TransferDetail detail = randomTransferDetail();

        // When
        TransferFrom transferFrom = new TransferFrom(detail);

        // Then
        assertThat(transferFrom.detail, equalTo(detail));
        assertThat(transferFrom.affectedAccountId(), equalTo(detail.fromAccountId));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new TransferFrom(null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("detail can't be null"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        TransferFrom transferFrom = new TransferFrom(randomTransferDetail());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(transferFrom)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = transferFrom.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }

    private TransferDetail randomTransferDetail() {
        return new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveAmount());
    }
}