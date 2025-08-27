package de.waldorfaugsburg.mensamax.server.service;

import de.waldorfaugsburg.mensamax.server.entity.MensaMaxTransactionEntity;
import de.waldorfaugsburg.mensamax.server.exception.*;
import de.waldorfaugsburg.mensamax.server.repository.TransactionRepository;
import de.waldorfaugsburg.mensamax.server.selenium.SeleniumClient;
import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import de.waldorfaugsburg.mensamax.transaction.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class TransactionService {

    private static final String KIOSK_CHIP_URL = MensaMaxService.URL + "/mensamax/Kiosk/Verkauf/VerkaufOeffnenForm.aspx";
    private static final String KIOSK_BARCODE_URL = MensaMaxService.URL + "/mensamax/Kiosk/Verkauf/VerkaufForm.aspx";
    private static final String KIOSK_SELECTOR_URL = MensaMaxService.URL + "/mensamax/grafik.aspx";

    private final MensaMaxService mensaMaxService;
    private final SeleniumService seleniumService;
    private final TransactionRepository repository;

    public TransactionService(final MensaMaxService mensaMaxService, final SeleniumService seleniumService, final TransactionRepository repository) {
        this.mensaMaxService = mensaMaxService;
        this.seleniumService = seleniumService;
        this.repository = repository;
    }

    @Scheduled(fixedRate = 10_000, initialDelay = 10_000)
    private void handlePendingTransactions() {
        for (final MensaMaxTransactionEntity transaction : repository.findAllByStatus(TransactionStatus.PENDING)) {
            try {
                performTransaction(transaction);
                transaction.setPerformedAt(Instant.now());
                transaction.setStatus(TransactionStatus.SUCCESS);
                repository.save(transaction);
            } catch (final Exception e) {
                transaction.setPerformedAt(Instant.now());
                transaction.setStatus(TransactionStatus.FAILED);
                repository.save(transaction);
            }
        }
    }

    public List<MensaMaxTransaction> fetchAllTransactions() {
        return StreamSupport.stream(repository.findAll().spliterator(), false).map(MensaMaxTransactionEntity::asModel).toList();
    }

    public MensaMaxTransaction fetchTransactionById(final long id) {
        return repository.findById(id).orElseThrow(TransactionNotFoundException::new).asModel();
    }

    public List<MensaMaxTransaction> fetchAllTransactionsByStatus(final TransactionStatus status) {
        return repository.streamAllByStatus(status).map(MensaMaxTransactionEntity::asModel).toList();
    }

    public void deleteTransactionById(final long id) {
        repository.deleteById(id);
    }

    public void recordTransaction(final String chip, final String kiosk, final long barcode, final int quantity) {
        final MensaMaxTransactionEntity entity = new MensaMaxTransactionEntity();
        entity.setUsername(mensaMaxService.findUsernameByChip(chip));
        entity.setChip(chip);
        entity.setKiosk(kiosk);
        entity.setBarcode(barcode);
        entity.setQuantity(quantity);
        entity.setStatus(TransactionStatus.PENDING);
        entity.setRecordedAt(Instant.now());
        repository.save(entity);
    }

    private void performTransaction(final MensaMaxTransactionEntity entity) {
        final SeleniumClient client = mensaMaxService.getClientStack().obtainClient();
        final WebDriver webDriver = client.getWebDriver();
        try {
            mensaMaxService.login(client);
            final String currentKiosk = client.getCurrentKiosk();
            if (currentKiosk == null || !currentKiosk.equals(entity.getKiosk())) {
                webDriver.get(KIOSK_SELECTOR_URL);

                final Select kioskSelect = new Select(seleniumService.waitUntilElementPresent(webDriver, By.id("cboKiosk")));
                for (int i = 0; i < kioskSelect.getOptions().size(); i++) {
                    final WebElement element = kioskSelect.getOptions().get(i);
                    if (element.getText().equalsIgnoreCase(entity.getKiosk())) {
                        kioskSelect.selectByIndex(i);
                        client.setCurrentKiosk(entity.getKiosk());
                        webDriver.switchTo().alert().accept();

                        log.info("Client '{}' switched to kiosk '{}'", client.getInstanceId(), entity.getKiosk());
                        break;
                    }
                }
            }

            webDriver.get(KIOSK_CHIP_URL);

            final WebElement identifierElement = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxBarcode"));
            identifierElement.sendKeys(entity.getChip(), Keys.ENTER);

            try {
                seleniumService.waitUntil(webDriver, ExpectedConditions.urlToBe(KIOSK_BARCODE_URL));
            } catch (final TimeoutException ignored) {
            }

            try {
                final WebElement statusElement = seleniumService.waitUntilElementPresent(webDriver, By.id("lblStatus"));
                throw new InvalidChipException(entity.getChip(), statusElement.getText());
            } catch (final TimeoutException ignored) {
            }

            for (int i = 0; i < entity.getQuantity(); i++) {
                final WebElement barcodeElement = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxBarcode"));
                barcodeElement.sendKeys(Long.toString(entity.getBarcode()), Keys.ENTER);

                final String sourceStringProductSelection = retrieveDialogSourceString(webDriver);
                if (sourceStringProductSelection != null) {
                    if (sourceStringProductSelection.contains("identifiziert")) {
                        throw new InvalidProductException(entity.getBarcode(), entity.getKiosk());
                    } else if (sourceStringProductSelection.contains("Kreditrahmen")) {
                        throw new AccountOverdrawnException();
                    } else if (sourceStringProductSelection.contains("Lagerbestand")) {
                        throw new NoStockException(entity.getBarcode(), entity.getKiosk());
                    } else if (sourceStringProductSelection.contains("Tageslimit")) {
                        throw new AccountDailyLimitException();
                    } else {
                        throw new UnknownErrorException(sourceStringProductSelection);
                    }
                }
            }

            // Let's spend some money
            seleniumService.click(webDriver, By.id("btnPay"));

            final String sourceStringPayment = retrieveDialogSourceString(webDriver);
            if (sourceStringPayment != null) {
                throw new UnknownErrorException(sourceStringPayment);
            }

            log.info("Processed transaction for product '{}' by user '{}' (Chip: {})", entity.getBarcode(), entity.getUsername(), entity.getChip());
        } catch (final Exception e) {
            log.error("An error occurred while performing transaction", e);
            throw e;
        } finally {
            mensaMaxService.getClientStack().returnClient(client);
        }
    }

    private String retrieveDialogSourceString(final WebDriver webDriver) {
        try {
            final List<WebElement> frames = seleniumService.waitUntilElementsPresent(webDriver, By.className("iFrameHinweis"));
            if (!frames.isEmpty()) {
                final WebElement frame = frames.getFirst();
                webDriver.switchTo().frame(frame);

                try {
                    final List<WebElement> contentItems = seleniumService.waitUntilElementsPresent(webDriver, By.className("hinweis-dialog-content"));
                    if (!contentItems.isEmpty()) {
                        final WebElement contentItem = contentItems.getFirst();
                        return contentItem.getText();
                    }
                } catch (TimeoutException ignored) {
                }

                try {
                    final List<WebElement> noticeItems = seleniumService.waitUntilElementsPresent(webDriver, By.className("TerminalHinweis"));
                    if (!noticeItems.isEmpty()) {
                        final WebElement noticeItem = noticeItems.getFirst();
                        return noticeItem.getText();
                    }
                } catch (TimeoutException ignored) {
                }
            }
        } catch (final TimeoutException ignored) {
        }

        webDriver.switchTo().defaultContent();
        return null;
    }
}