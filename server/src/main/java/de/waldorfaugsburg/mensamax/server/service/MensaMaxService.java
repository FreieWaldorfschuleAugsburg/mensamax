package de.waldorfaugsburg.mensamax.server.service;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import de.waldorfaugsburg.mensamax.server.configuration.MensaMaxConfigurationProperties;
import de.waldorfaugsburg.mensamax.server.exception.*;
import de.waldorfaugsburg.mensamax.server.selenium.SeleniumClient;
import de.waldorfaugsburg.mensamax.server.selenium.SeleniumClientStack;
import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MensaMaxService {
    private static final String URL = "https://mensastadt.de";
    private static final String LOGIN_URL = URL + "/?projekt=%s&einrichtung=%s&user=%s";
    private static final String INDEX_URL = URL + "/mensamax/index.aspx";
    private static final String KIOSK_SELECTOR_URL = URL + "/mensamax/grafik.aspx";
    private static final String KIOSK_CHIP_URL = URL + "/mensamax/Kiosk/Verkauf/VerkaufOeffnenForm.aspx";
    private static final String KIOSK_BARCODE_URL = URL + "/mensamax/Kiosk/Verkauf/VerkaufForm.aspx";
    private static final String SEARCH_URL = URL + "/mensamax/Formulare/Person/PersonSucheForm.aspx";
    private static final String DATA_URL = URL + "/mensamax/Formulare/Person/PersonDatenForm.aspx";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String[] CSV_HEADERS = {"id", "datetime", "chip", "kiosk", "barcode", "quantity"};

    private final LoadingCache<String, MensaMaxUser> userCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public @NotNull MensaMaxUser load(@NotNull final String chip) throws InvalidChipException {
            final MensaMaxUser response = requestUserByChip(chip);
            if (response == null) throw new InvalidChipException(chip);

            return response;
        }
    });

    private final MensaMaxConfigurationProperties properties;

    private final SeleniumService seleniumService;
    private SeleniumClientStack clientStack;

    private final TransactionService transactionService;

    public MensaMaxService(final MensaMaxConfigurationProperties properties, final SeleniumService seleniumService, final TransactionService transactionService) {
        this.properties = properties;
        this.seleniumService = seleniumService;
        this.transactionService = transactionService;
        if (properties.online()) {
            this.clientStack = seleniumService.reserveClients(properties.clientCount(), this::login);
        }
    }

    public MensaMaxUser getUserByChip(final String chip) throws InvalidChipException {
        try {
            return userCache.get(chip);
        } catch (final Exception e) {
            throw (RuntimeException) e.getCause();
        }
    }

    public MensaMaxUser requestUserByChip(final String chip) {
        Preconditions.checkNotNull(chip, "chip may not be null");

        final SeleniumClient client = clientStack.obtainClient();
        final WebDriver webDriver = client.getWebDriver();
        try {
            login(client);
            webDriver.get(SEARCH_URL);

            seleniumService.click(webDriver, By.id("btnBarcodeSearch"));
            final WebElement barcodeElement = webDriver.switchTo().activeElement();
            barcodeElement.sendKeys(chip, Keys.ENTER);

            log.info("Requested user by chip id '{}'", chip);
            return readUserData(webDriver);
        } catch (final Exception e) {
            log.error("An error occurred while requesting user by chip id {}", chip, e);
            throw new InvalidChipException(chip, e);
        } finally {
            clientStack.returnClient(client);
        }
    }

    public void transaction(final UUID id, final String chip, final String kiosk, final long barcode, final int quantity) {
        Preconditions.checkNotNull(chip, "chip may not be null");
        Preconditions.checkArgument(quantity > 0, "quantity must be bigger than zero");

        // Check whether server runs in online mode
        if (properties.online()) {
            // Check if product is restricted
            if (properties.restrictedProducts().contains(barcode)) {
                final MensaMaxUser user = getUserByChip(chip);
                final Set<String> restrictedRoles = properties.restrictedRoles().get(kiosk);
                if (restrictedRoles != null && restrictedRoles.contains(user.getUserGroup())) {
                    throw new ProductRestrictedException();
                }
            }

            onlineTransaction(chip, kiosk, barcode, quantity);
        } else {
            offlineTransaction(id, chip, kiosk, barcode, quantity);
        }

        // Save transaction for further analysis
        transactionService.saveTransaction(new MensaMaxTransaction(id, chip, barcode, System.currentTimeMillis()));
    }

    private void onlineTransaction(final String chip, final String kiosk, final long barcode, final int quantity) {
        final SeleniumClient client = clientStack.obtainClient();
        final WebDriver webDriver = client.getWebDriver();
        try {
            login(client);
            final String currentKiosk = client.getCurrentKiosk();
            if (currentKiosk == null || !currentKiosk.equals(kiosk)) {
                webDriver.get(KIOSK_SELECTOR_URL);

                final Select kioskSelect = new Select(seleniumService.waitUntilElementPresent(webDriver, By.id("cboKiosk")));
                for (int i = 0; i < kioskSelect.getOptions().size(); i++) {
                    final WebElement element = kioskSelect.getOptions().get(i);
                    if (element.getText().equalsIgnoreCase(kiosk)) {
                        kioskSelect.selectByIndex(i);
                        client.setCurrentKiosk(kiosk);
                        webDriver.switchTo().alert().accept();

                        log.info("Client '{}' switched to kiosk '{}'", client.getInstanceId(), kiosk);
                        break;
                    }
                }
            }

            webDriver.get(KIOSK_CHIP_URL);

            final WebElement identifierElement = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxBarcode"));
            identifierElement.sendKeys(chip, Keys.ENTER);

            try {
                seleniumService.waitUntil(webDriver, ExpectedConditions.urlToBe(KIOSK_BARCODE_URL));
            } catch (final TimeoutException ignored) {
            }

            try {
                final WebElement statusElement = seleniumService.waitUntilElementPresent(webDriver, By.id("lblStatus"));
                throw new InvalidChipException(chip, statusElement.getText());
            } catch (final TimeoutException ignored) {
            }

            for (int i = 0; i < quantity; i++) {
                final WebElement barcodeElement = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxBarcode"));
                barcodeElement.sendKeys(Long.toString(barcode), Keys.ENTER);

                final String sourceStringProductSelection = retrieveDialogSourceString(webDriver);
                if (sourceStringProductSelection != null) {
                    if (sourceStringProductSelection.contains("identifiziert")) {
                        throw new InvalidProductException(barcode, kiosk);
                    } else if (sourceStringProductSelection.contains("Kreditrahmen")) {
                        throw new AccountOverdrawnException();
                    } else if (sourceStringProductSelection.contains("Lagerbestand")) {
                        throw new NoStockException(barcode, kiosk);
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

            log.info("Processed transaction for product '{}' by chip id '{}'", barcode, chip);
        } catch (final Exception e) {
            log.error("An error occurred while performing transaction", e);
            throw e;
        } finally {
            clientStack.returnClient(client);
        }
    }

    private void offlineTransaction(final UUID id, final String chip, final String kiosk, final long barcode, final int quantity) {
        try {
            boolean skipHeader = true;
            final File file = new File("transactions_" + DATE_FORMAT.format(new Date()) + ".csv");
            if (!file.exists()) {
                skipHeader = false;
                Files.createFile(file.toPath());
            }

            try (final FileWriter writer = new FileWriter(file, true)) {
                final CSVFormat format = CSVFormat.DEFAULT.builder()
                        .setHeader(CSV_HEADERS)
                        .setSkipHeaderRecord(skipHeader)
                        .build();

                try (final CSVPrinter printer = new CSVPrinter(writer, format)) {
                    printer.printRecord(id, System.currentTimeMillis(), chip, kiosk, barcode, quantity);
                }
            }
            log.info("Processed offline transaction for product '{}' by chip id '{}'", barcode, chip);
        } catch (final IOException e) {
            log.error("An error occurred while saving transaction", e);
        }
    }

    private void login(final SeleniumClient client) throws LoginException {
        login(client, properties.username(), properties.password());
    }

    private void login(final SeleniumClient client, final String username, final String password) throws LoginException {
        final long lastActionDate = client.getLastActionDate();
        if (System.currentTimeMillis() - lastActionDate < TimeUnit.HOURS.toMillis(1)) {
            return;
        }

        final WebDriver webDriver = client.getWebDriver();
        webDriver.get(INDEX_URL);

        try {
            seleniumService.waitUntil(webDriver, ExpectedConditions.urlMatches("^((?!\bCustErrors.aspx\b).)*$"));

            // Opening login page
            webDriver.get(String.format(LOGIN_URL, properties.projectId(), properties.facilityId(), properties.username()));

            seleniumService.clearAndSendKeys(webDriver, By.id("tbxKennwort"), password);

            // Press login
            seleniumService.click(webDriver, By.id("btnLogin"));

            // Wait till login is finished
            seleniumService.waitUntil(webDriver, ExpectedConditions.urlToBe(INDEX_URL));

            // Set current kiosk name as property
            webDriver.get(KIOSK_SELECTOR_URL);

            final Select kioskSelect = new Select(seleniumService.waitUntilElementPresent(webDriver, By.id("cboKiosk")));
            client.setCurrentKiosk(kioskSelect.getFirstSelectedOption().getText());

            client.setLastActionDate(System.currentTimeMillis());
            log.info("Client '{}' successfully logged in as '{}'", client.getInstanceId(), username);

        } catch (final TimeoutException e) {
            try {
                final WebElement element = seleniumService.waitUntilElementPresent(webDriver, By.id("lblHinweis"));
                throw new LoginException(element.getText());
            } catch (TimeoutException ignored) {
            }
            throw new LoginException(e);
        }
    }

    private MensaMaxUser readUserData(final WebDriver webDriver) throws TimeoutException {
        seleniumService.waitUntil(webDriver, ExpectedConditions.or(ExpectedConditions.urlMatches(DATA_URL)));

        final String username = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxBenutzername")).getAttribute("value");
        final String firstName = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxVorname")).getAttribute("value");
        final String lastName = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxNachname")).getAttribute("value");
        final String dateOfBirth = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxGebDatum")).getAttribute("value");
        final String email = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxEmail")).getAttribute("value");
        final String userGroup = new Select(seleniumService.waitUntilElementPresent(webDriver, By.id("cboKlasse"))).getFirstSelectedOption().getText();

        return new MensaMaxUser(username, firstName, lastName, email, dateOfBirth, userGroup);
    }

    private String retrieveDialogSourceString(final WebDriver webDriver) {
        try {
            final List<WebElement> frames = seleniumService.waitUntilElementsPresent(webDriver, By.className("iFrameHinweis"));
            if (!frames.isEmpty()) {
                final WebElement frame = frames.get(0);
                webDriver.switchTo().frame(frame);

                try {
                    final List<WebElement> contentItems = seleniumService.waitUntilElementsPresent(webDriver, By.className("hinweis-dialog-content"));
                    if (!contentItems.isEmpty()) {
                        final WebElement contentItem = contentItems.get(0);
                        return contentItem.getText();
                    }
                } catch (TimeoutException ignored) {
                }

                try {
                    final List<WebElement> noticeItems = seleniumService.waitUntilElementsPresent(webDriver, By.className("TerminalHinweis"));
                    if (!noticeItems.isEmpty()) {
                        final WebElement noticeItem = noticeItems.get(0);
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
