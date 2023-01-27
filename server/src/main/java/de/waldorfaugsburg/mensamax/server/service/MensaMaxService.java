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
import de.waldorfaugsburg.mensamax.transaction.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private static final String IDENTIFICATION_URL = URL + "/mensamax/Formulare/Person/PersonIdentifikationForm.aspx";

    private final LoadingCache<String, MensaMaxUser> chipUserCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public @NotNull MensaMaxUser load(@NotNull final String chip) throws InvalidChipException {
            final MensaMaxUser response = requestUserByChip(chip);
            if (response == null) throw new InvalidChipException(chip);

            return response;
        }
    });

    private final LoadingCache<String, MensaMaxUser> usernameUserCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public @NotNull MensaMaxUser load(@NotNull final String username) throws InvalidUsernameException {
            final MensaMaxUser response = requestUserByUsername(username);
            if (response == null) throw new InvalidUsernameException(username);

            return response;
        }
    });

    private final MensaMaxConfigurationProperties properties;

    private final SeleniumService seleniumService;
    private final SeleniumClientStack clientStack;

    public MensaMaxService(final MensaMaxConfigurationProperties properties, final SeleniumService seleniumService) {
        this.properties = properties;
        this.seleniumService = seleniumService;
        this.clientStack = seleniumService.reserveClients(properties.clientCount(), this::login);
    }

    public MensaMaxUser getUserByUsername(final String username) throws InvalidUsernameException {
        try {
            final MensaMaxUser user = usernameUserCache.get(username);

            // Update user in the other cache
            user.getChipIds().forEach(chipId -> chipUserCache.put(chipId, user));
            return user;
        } catch (final Exception e) {
            throw (RuntimeException) e.getCause();
        }
    }

    public MensaMaxUser getUserByChip(final String chip) throws InvalidChipException {
        try {
            final MensaMaxUser user = chipUserCache.get(chip);

            // Update user in the other cache
            usernameUserCache.put(user.getUsername(), user);
            return user;
        } catch (final Exception e) {
            throw (RuntimeException) e.getCause();
        }
    }

    public MensaMaxUser requestUserByUsername(final String username) {
        Preconditions.checkNotNull(username, "username may not be null");

        final SeleniumClient client = clientStack.obtainClient();
        final WebDriver webDriver = client.getWebDriver();
        try {
            login(client);
            webDriver.get(SEARCH_URL);

            seleniumService.clearAndSendKeys(webDriver, By.id("tbloginname"), username, Keys.ENTER);

            log.info("Requested user by username '{}'", username);
            return readUserData(webDriver);
        } catch (final Exception e) {
            log.error("An error occurred while requesting user by username {}", username, e);
            throw new InvalidUsernameException(username, e);
        } finally {
            clientStack.returnClient(client);
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

    public MensaMaxTransaction transaction(final String transactionId, final String chip, final String kiosk, final long barcode, final int quantity) {
        Preconditions.checkNotNull(chip, "chip may not be null");
        Preconditions.checkArgument(quantity > 0, "quantity must be bigger than zero");

        if (properties.restrictedProducts().contains(barcode)) {
            final MensaMaxUser user = getUserByChip(chip);
            final Set<String> restrictedRoles = properties.restrictedRoles().get(kiosk);
            if (restrictedRoles != null && restrictedRoles.contains(user.getUserGroup())) {
                throw new ProductRestrictedException();
            }
        }

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
        return new MensaMaxTransaction(transactionId, getUserByChip(chip), barcode, TransactionStatus.SUCCESS, LocalDateTime.now());
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

        webDriver.get(IDENTIFICATION_URL);

        final int rowCount = seleniumService.waitUntilElementsPresent(webDriver, By.xpath("//table[@id='tblIdent']/tbody/tr")).size();
        final Set<String> chipIds = new HashSet<>();
        for (int i = 1; i <= rowCount; i++) {
            final String chipId = webDriver.findElement(By.xpath("//table[@id='tblIdent']/tbody/tr[" + i + "]/td[2]")).getText();
            chipIds.add(chipId);
        }

        return new MensaMaxUser(username, firstName, lastName, email, dateOfBirth, userGroup, chipIds);
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
