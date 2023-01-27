package unit;

import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import de.waldorfaugsburg.mensamax.server.service.TransactionService;
import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import de.waldorfaugsburg.mensamax.transaction.TransactionStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TransactionServiceUnitTests {
    @MockBean
    private TransactionService service;


    @Before
    public void setUp() {
        this.service = new TransactionService();
    }


    @Test
    public void testTransactionLogging() {
        MensaMaxTransaction transaction = getMensaMaxTransaction(LocalDateTime.now());
        final String chipId = "AWDWDA";
        service.logTransaction(transaction, chipId);
        MensaMaxTransaction loggedTransaction = this.service.getTransaction(transaction.getTransactionId());
        assertEquals(transaction.getTransactionId(), loggedTransaction.getTransactionId());
    }

    @Test
    public void testSuspiciousTransactionDetection() {
        final String chipId = "AWDWDA";
        MensaMaxTransaction transaction1 = getMensaMaxTransaction(LocalDateTime.now());
        MensaMaxTransaction transaction2 = getMensaMaxTransaction(LocalDateTime.now().plusSeconds(30));
        MensaMaxTransaction transaction3 = getMensaMaxTransaction(LocalDateTime.now().plusSeconds(80));
        service.logTransaction(transaction1, chipId);
        service.logTransaction(transaction2, chipId);
        service.logTransaction(transaction3, chipId);
        Set<MensaMaxTransaction> suspiciousTransactions = service.getSuspiciousTransactions();
        assertEquals(1, suspiciousTransactions.size());
    }

    private MensaMaxTransaction getMensaMaxTransaction(LocalDateTime transactionDate) {
        final String transactionId = UUID.randomUUID().toString();
        final long barcode = 1337;
        final TransactionStatus status = TransactionStatus.SUCCESS;
        return new MensaMaxTransaction(transactionId, getMensaMaxUser(), barcode, status, transactionDate);
    }


    private MensaMaxUser getMensaMaxUser() {
        final String username = "mmxMaxMustermann";
        final String firstName = "Max";
        final String lastName = "Mustermann";
        final String email = "max@mustermann.de";
        final String dateOfBirth = "02.02.1970";
        final String userGroup = "Klasse 12";
        final Set<String> chipIds = new HashSet<String>();
        chipIds.add("ABCDEFG");
        return new MensaMaxUser(username, firstName, lastName, email, dateOfBirth, userGroup, chipIds);
    }


}
