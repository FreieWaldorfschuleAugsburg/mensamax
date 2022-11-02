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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MensaMaxService {

    private static final String URL = "https://mensastadt.de";
    private static final String LOGIN_URL = URL + "/Login.aspx";
    private static final String INDEX_URL = URL + "/mensamax/index.aspx";
    private static final String KIOSK_SELECTOR_URL = URL + "/mensamax/grafik.aspx";
    private static final String KIOSK_URL = URL + "/mensamax/Kiosk/Verkauf/VerkaufOeffnenForm.aspx";
    private static final String SEARCH_URL = URL + "/mensamax/Formulare/Person/PersonSucheForm.aspx";
    private static final String DATA_ADMIN_URL = URL + "/mensamax/Formulare/Person/PersonDatenForm.aspx";
    private static final String DATA_SELF_URL = URL + "/mensamax/meinedaten/benutzerdatenform.aspx";
    private static final String IDENTIFICATION_ADMIN_URL = URL + "/mensamax/Formulare/Person/PersonIdentifikationForm.aspx";
    private static final String IDENTIFICATION_SELF_URL = URL + "/mensamax/meinedaten/MeineIdentifikation.aspx";
    private static final String LOGOUT_URL = URL + "/mensamax/logout.aspx";

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

    public MensaMaxService(final MensaMaxConfigurationProperties properties,
                           final SeleniumService seleniumService) {
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
            seleniumService.waitUntilPageReady(webDriver);

            seleniumService.clearAndSendKeys(webDriver, "tbloginname", username, Keys.ENTER);
            seleniumService.waitUntilPageReady(webDriver);

            log.info("Requested user by username '{}'", username);
            return readUserData(webDriver);
        } catch (final Exception e) {
            log.error("An error occurred while requesting user by username {}", username, e);
            throw e;
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
            seleniumService.waitUntilPageReady(webDriver);

            webDriver.findElement(By.id("btnBarcodeSearch")).click();
            final WebElement barcodeElement = webDriver.switchTo().activeElement();
            barcodeElement.sendKeys(chip, Keys.ENTER);
            seleniumService.waitUntilPageReady(webDriver);

            log.info("Requested user by chip id '{}'", chip);
            return readUserData(webDriver);
        } catch (final Exception e) {
            log.error("An error occurred while requesting user by chip id {}", chip, e);
            throw e;
        } finally {
            clientStack.returnClient(client);
        }
    }

    public void transaction(final String chipId, final String kiosk, final long productBarcode, final int quantity) {
        Preconditions.checkNotNull(chipId, "chipId may not be null");
        Preconditions.checkArgument(quantity > 0, "quantity must be bigger than zero");

        if (properties.restrictedProducts().contains(productBarcode)) {
            final MensaMaxUser user = getUserByChip(chipId);
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
                seleniumService.waitUntilPageReady(webDriver);

                final Select kioskSelect = new Select(webDriver.findElement(By.id("cboKiosk")));
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
                seleniumService.waitUntilPageReady(webDriver);
            }

            webDriver.get(KIOSK_URL);
            seleniumService.waitUntilPageReady(webDriver);

            final WebElement identifierElement = webDriver.switchTo().activeElement();
            identifierElement.sendKeys(chipId, Keys.ENTER);
            seleniumService.waitUntilPageReady(webDriver);

            if (!webDriver.findElements(By.id("lblStatus")).isEmpty()) {
                throw new InvalidChipException(chipId);
            }

            for (int i = 0; i < quantity; i++) {
                final WebElement barcodeElement = webDriver.switchTo().activeElement();
                barcodeElement.sendKeys(Long.toString(productBarcode), Keys.ENTER);
                seleniumService.waitUntilPageReady(webDriver);

                final String sourceStringProductSelection = retrieveDialogSourceString(webDriver);
                if (sourceStringProductSelection != null) {
                    if (sourceStringProductSelection.contains("identifiziert")) {
                        throw new InvalidProductException(productBarcode, kiosk);
                    } else if (sourceStringProductSelection.contains("Kreditrahmen")) {
                        throw new AccountOverdrawnException();
                    } else if (sourceStringProductSelection.contains("Lagerbestand")) {
                        throw new NoStockException(productBarcode, kiosk);
                    } else if (sourceStringProductSelection.contains("Tageslimit")) {
                        throw new AccountDailyLimitException();
                    } else {
                        throw new UnknownErrorException(sourceStringProductSelection);
                    }
                }
            }

            webDriver.findElement(By.name("btnPay")).click();
            seleniumService.waitUntilPageReady(webDriver);

            final String sourceStringPayment = retrieveDialogSourceString(webDriver);
            if (sourceStringPayment != null) {
                throw new UnknownErrorException(sourceStringPayment);
            }

            log.info("Processed transaction for product '{}' by chip id '{}'", productBarcode, chipId);
        } catch (final Exception e) {
            log.error("An error occurred while performing transaction", e);
            throw e;
        } finally {
            clientStack.returnClient(client);
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
        seleniumService.waitUntilPageReady(webDriver);

        // Check if already logged in
        if (!webDriver.getCurrentUrl().contains("CustErrors.aspx")) {
            return;
        }

        // Opening login page
        webDriver.get(URL);
        seleniumService.waitUntilPageReady(webDriver);

        seleniumService.clearAndSendKeys(webDriver, "tbxProjekt", properties.projectId(), Keys.TAB);

        // Waiting for project verification to be finished (weird page "reload" although data provider is already correct)
        seleniumService.waitUntilPageReady(webDriver);
        seleniumService.clearAndSendKeys(webDriver, "tbxEinrichtung", properties.facilityId());
        seleniumService.clearAndSendKeys(webDriver, "tbxBenutzername", username);
        seleniumService.clearAndSendKeys(webDriver, "tbxKennwort", password);

        // Press login
        webDriver.findElement(By.name("btnLogin")).click();
        seleniumService.waitUntilPageReady(webDriver);

        if (!webDriver.getCurrentUrl().equals(INDEX_URL)) {
            throw new LoginException();
        }

        // Set current kiosk name as property
        webDriver.get(KIOSK_SELECTOR_URL);
        seleniumService.waitUntilPageReady(webDriver);
        final Select kioskSelect = new Select(webDriver.findElement(By.id("cboKiosk")));
        client.setCurrentKiosk(kioskSelect.getFirstSelectedOption().getText());

        client.setLastActionDate(System.currentTimeMillis());
        log.info("Client '{}' successfully logged in as '{}'", client.getInstanceId(), username);
    }

    private void logout(final SeleniumClient client) throws LogoutException {
        client.setLastActionDate(0);

        final WebDriver webDriver = client.getWebDriver();
        webDriver.get(LOGOUT_URL);
        seleniumService.waitUntilPageReady(webDriver);

        if (!webDriver.getCurrentUrl().equals(LOGIN_URL)) {
            throw new LogoutException();
        }

        log.info("Client '{}' successfully logged out!", client.getInstanceId());
    }

    private MensaMaxUser readUserData(final WebDriver webDriver) {
        final String currentUrl = webDriver.getCurrentUrl();
        if (currentUrl.equals(SEARCH_URL)) return null;

        final String username;
        final String firstName;
        final String lastName;
        final String dateOfBirth;
        final String email;
        final String userGroup;
        if (currentUrl.startsWith(DATA_SELF_URL)) {
            username = webDriver.findElement(By.id("tbxLoginname")).getAttribute("value");
            firstName = webDriver.findElement(By.id("tbxVorname")).getAttribute("value");
            lastName = webDriver.findElement(By.id("tbxNachname")).getAttribute("value");
            dateOfBirth = webDriver.findElement(By.id("tbxGebDatum")).getAttribute("value");
            email = webDriver.findElement(By.id("tbxPWvergessen")).getAttribute("value");
            userGroup = webDriver.findElement(By.id("tbxKlasse")).getAttribute("value");

            webDriver.get(IDENTIFICATION_SELF_URL);
        } else if (currentUrl.startsWith(DATA_ADMIN_URL)) {
            username = webDriver.findElement(By.id("tbxBenutzername")).getAttribute("value");
            firstName = webDriver.findElement(By.id("tbxVorname")).getAttribute("value");
            lastName = webDriver.findElement(By.id("tbxNachname")).getAttribute("value");
            dateOfBirth = webDriver.findElement(By.id("tbxGebDatum")).getAttribute("value");
            email = webDriver.findElement(By.id("tbxEmail")).getAttribute("value");
            userGroup = new Select(webDriver.findElement(By.id("cboKlasse"))).getFirstSelectedOption().getText();

            webDriver.get(IDENTIFICATION_ADMIN_URL);
        } else {
            throw new IllegalStateException("invalid page " + webDriver.getCurrentUrl());
        }

        seleniumService.waitUntilPageReady(webDriver);

        final int rowCount = webDriver.findElements(By.xpath("//table[@id='tblIdent']/tbody/tr")).size();
        final Set<String> chipIds = new HashSet<>();
        for (int i = 1; i <= rowCount; i++) {
            final String chipId = webDriver.findElement(By.xpath("//table[@id='tblIdent']/tbody/tr[" + i + "]/td[2]")).getText();
            chipIds.add(chipId);
        }

        return new MensaMaxUser(username, firstName, lastName, email, dateOfBirth, userGroup, chipIds);
    }

    private String retrieveDialogSourceString(final WebDriver webDriver) {
        final List<WebElement> frames = webDriver.findElements(By.className("iFrameHinweis"));
        if (!frames.isEmpty()) {
            final WebElement frame = frames.get(0);
            webDriver.switchTo().frame(frame);
            seleniumService.waitUntilPageReady(webDriver);

            final List<WebElement> contentItems = webDriver.findElements(By.className("hinweis-dialog-content"));
            if (!contentItems.isEmpty()) {
                final WebElement contentItem = contentItems.get(0);
                return contentItem.getText();
            }

            final List<WebElement> noticeItems = webDriver.findElements(By.className("TerminalHinweis"));
            if (!noticeItems.isEmpty()) {
                final WebElement noticeItem = noticeItems.get(0);
                return noticeItem.getText();
            }
        }

        webDriver.switchTo().defaultContent();
        return null;
    }
}
