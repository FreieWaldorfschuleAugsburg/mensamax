package unit;

import de.waldorfaugsburg.mensamax.server.service.TransactionService;
import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TransactionServiceUnitTests {

    @MockBean
    private TransactionService service;

    @Before
    public void setUp() {
        this.service = new TransactionService();
    }

    @Test
    public void testTransactionLogging() {
        final MensaMaxTransaction firstTransaction = createTransaction();
        final MensaMaxTransaction secondTransaction = createTransaction();
        service.saveTransaction(firstTransaction);
        service.saveTransaction(secondTransaction);

        assertEquals(service.getTransactions(firstTransaction.getChip()).size(), 2);

        final MensaMaxTransaction lastTransaction = service.getLastTransaction(firstTransaction.getChip());
        assertNotEquals(lastTransaction.getId(), firstTransaction.getId());
        assertEquals(lastTransaction.getId(), secondTransaction.getId());
    }

    private MensaMaxTransaction createTransaction() {
        return new MensaMaxTransaction(UUID.randomUUID(), "ABCDEFGHIJ", 1337, System.currentTimeMillis());
    }
}
