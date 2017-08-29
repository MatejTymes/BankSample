package mtymes.account.domain.operation;

import mtymes.test.StrictMockTest;
import org.junit.Test;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javafixes.common.CollectionUtil.newSet;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransferToTest extends StrictMockTest {

    @Test
    public void shouldCreateOperation() {
        TransferDetail detail = randomTransferDetail();

        // When
        TransferTo transferTo = new TransferTo(detail);

        // Then
        assertThat(transferTo.detail, equalTo(detail));
        assertThat(transferTo.affectedAccountId(), equalTo(newSet(detail.toAccountId)));
    }

    @Test
    public void shouldFailConstructionOnInvalidParameters() {
        try {
            new TransferTo(null);

            fail("should fail with NullPointerException");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), equalTo("detail can't be null"));
        }
    }

    @Test
    public void shouldCallCorrectVisitorMethod() {
        OperationVisitor<UUID> visitor = mock(OperationVisitor.class);
        TransferTo transferTo = new TransferTo(randomTransferDetail());

        UUID expectedResponse = randomUUID();
        when(visitor.visit(transferTo)).thenReturn(expectedResponse);

        // When
        UUID actualResponse = transferTo.apply(visitor);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }

    private TransferDetail randomTransferDetail() {
        return new TransferDetail(randomTransferId(), randomAccountId(), randomAccountId(), randomPositiveDecimal());
    }

}