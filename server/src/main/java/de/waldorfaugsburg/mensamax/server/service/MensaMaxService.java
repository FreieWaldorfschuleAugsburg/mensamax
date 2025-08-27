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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MensaMaxService {

    public static final String URL = "https://mensastadt.de";
    private static final String LOGIN_URL = URL + "/?projekt=%s&einrichtung=%s&user=%s";
    private static final String INDEX_URL = URL + "/mensamax/index.aspx";
    private static final String PERSON_SEARCH_URL = URL + "/mensamax/Formulare/Person/PersonSucheForm.aspx";
    private static final String PERSON_DATA_URL = URL + "/mensamax/Formulare/Person/PersonDatenForm.aspx";
    private static final String PERSON_ROLE_URL = URL + "/mensamax/Formulare/Person/PersonRolleForm.aspx";
    private static final String PERSON_EMAIL_URL = URL + "/mensamax/Formulare/Person/PersonEMailForm.aspx";
    private static final String PERSON_CREATE_URL = URL + "/mensamax/Formulare/Person/PersonDatenForm.aspx?PersonID=0";

    private final LoadingCache<@NotNull String, @NotNull MensaMaxUser> userUsernameCache = CacheBuilder.newBuilder().expireAfterWrite(8, TimeUnit.HOURS).build(new CacheLoader<>() {
        @Override
        public @NotNull MensaMaxUser load(@NotNull final String chip) throws InvalidChipException {
            final MensaMaxUser response = findUserByUsername(chip);

            // Update other cache
            userEmployeeIdCache.put(response.getEmployeeId(), response);
            return response;
        }
    });

    private final LoadingCache<@NotNull Integer, @NotNull MensaMaxUser> userEmployeeIdCache = CacheBuilder.newBuilder().expireAfterWrite(8, TimeUnit.HOURS).build(new CacheLoader<>() {
        @Override
        public @NotNull MensaMaxUser load(@NotNull final Integer employeeId) throws InvalidChipException {
            final MensaMaxUser response = findUserByEmployeeId(employeeId);

            // Update other cache
            userUsernameCache.put(response.getUsername(), response);
            return response;
        }
    });

    private final MensaMaxConfigurationProperties properties;
    private final SeleniumService seleniumService;
    @Getter
    private final SeleniumClientStack clientStack;

    public MensaMaxService(final MensaMaxConfigurationProperties properties, final SeleniumService seleniumService) {
        this.properties = properties;
        this.seleniumService = seleniumService;
        this.clientStack = seleniumService.reserveClients(properties.clientCount(), this::login);
    }

    public MensaMaxUser getUserByUsername(final String username) throws InvalidFieldException {
        try {
            return userUsernameCache.get(username);
        } catch (final Exception e) {
            throw (RuntimeException) e.getCause();
        }
    }

    public MensaMaxUser getUserByEmployeeId(final int employeeId) throws InvalidFieldException {
        try {
            return userEmployeeIdCache.get(employeeId);
        } catch (final Exception e) {
            throw (RuntimeException) e.getCause();
        }
    }

    public String findUsernameByChip(final String chip) throws InvalidChipException {
        Preconditions.checkNotNull(chip, "chip may not be null");

        final SeleniumClient client = clientStack.obtainClient();
        final WebDriver webDriver = client.getWebDriver();
        try {
            login(client);
            webDriver.get(PERSON_SEARCH_URL);

            seleniumService.click(webDriver, By.id("btnBarcodeSearch"));
            final WebElement barcodeElement = webDriver.switchTo().activeElement();
            barcodeElement.sendKeys(chip, Keys.ENTER);

            seleniumService.waitUntil(webDriver, ExpectedConditions.urlMatches(PERSON_DATA_URL));
            final String username = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxBenutzername")).getDomAttribute("value");
            log.info("Requested username '{}' by chip id '{}'", username, chip);
            return username;
        } catch (final TimeoutException e) {
            log.error("Couldn't find username for invalid chip '{}'", chip);
            throw new InvalidChipException(chip);
        } catch (final Exception e) {
            log.error("An error occurred while requesting user by chip id {}", chip, e);
            throw new UnknownErrorException(e);
        } finally {
            clientStack.returnClient(client);
        }
    }

    public MensaMaxUser findUserByUsername(final String username) throws InvalidFieldException {
        return findUserByInputField("tbloginname", username);
    }

    public MensaMaxUser findUserByEmployeeId(final int employeeId) throws InvalidFieldException {
        return findUserByInputField("tbxPersonalnummer", Integer.toString(employeeId));
    }

    public MensaMaxUser findUserByInputField(final String inputFieldName, final String value) throws InvalidFieldException {
        Preconditions.checkNotNull(inputFieldName, "inputFieldName may not be null");
        Preconditions.checkNotNull(value, "value may not be null");

        final SeleniumClient client = clientStack.obtainClient();
        final WebDriver webDriver = client.getWebDriver();
        try {
            login(client);
            webDriver.get(PERSON_SEARCH_URL);

            seleniumService.clearAndSendKeys(webDriver, By.id(inputFieldName), value, Keys.ENTER);

            log.info("Requested user by field '{}' with value '{}'", inputFieldName, value);
            return readUserData(webDriver);
        } catch (final Exception e) {
            log.error("An error occurred while requesting user field '{}' with value '{}'", inputFieldName, value, e);
            throw new InvalidFieldException(inputFieldName, value, e);
        } finally {
            clientStack.returnClient(client);
        }
    }

    public void createUser(final MensaMaxUser user) {
        Preconditions.checkNotNull(user, "user may not be null");

        final SeleniumClient client = clientStack.obtainClient();
        final WebDriver webDriver = client.getWebDriver();
        try {
            login(client);

            try {
                getUserByUsername(user.getUsername());
                throw new UserAlreadyExistsException(user.getUsername());
            } catch (final Exception ignored) {
            }

            webDriver.get(PERSON_CREATE_URL);
            writeUserData(webDriver, user);

            log.info("Created user '{}''", user.getUsername());
        } catch (final Exception e) {
            log.error("An error occurred while creating user '{}'", user.getUsername(), e);
            throw new UserCreationException(user.getUsername(), e);
        } finally {
            clientStack.returnClient(client);
        }
    }

    public void updateUser(final MensaMaxUser user) {
        Preconditions.checkNotNull(user, "user may not be null");

    }

    public void login(final SeleniumClient client) throws LoginException {
        login(client, properties.username(), properties.password());
    }

    public void login(final SeleniumClient client, final String username, final String password) throws LoginException {
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

    private void writeUserData(final WebDriver webDriver, final MensaMaxUser user) {
        seleniumService.waitUntil(webDriver, ExpectedConditions.or(ExpectedConditions.urlContains(PERSON_DATA_URL),
                ExpectedConditions.urlMatches(PERSON_CREATE_URL)));

        seleniumService.clearAndSendKeys(webDriver, By.id("tbxBenutzername"), user.getUsername());
        seleniumService.clearAndSendKeys(webDriver, By.id("tbxNachname"), user.getLastName());
        seleniumService.clearAndSendKeys(webDriver, By.id("tbxVorname"), user.getFirstName());
        seleniumService.clearAndSendKeys(webDriver, By.id("tbxGebDatum"), user.getDateOfBirth());
        seleniumService.clearAndSendKeys(webDriver, By.id("tbxPersNr"), Integer.toString(user.getEmployeeId()));
        seleniumService.clearAndSendKeys(webDriver, By.id("tbxEmail"), user.getEmail());

        seleniumService.click(webDriver, By.id("btnSpeichernPerson"));

        // Wait until group selection is fully populated
        seleniumService.waitUntil(webDriver, () -> new Select(seleniumService.waitUntilElementPresent(webDriver, By.id("cboKlasse"))).getOptions().size() > 1);

        final Select userGroupSelector = new Select(seleniumService.waitUntilElementPresent(webDriver, By.id("cboKlasse")));
        userGroupSelector.selectByVisibleText(user.getUserGroup());
        seleniumService.click(webDriver, By.id("btnSpeichernPerson"));

        seleniumService.waitUntil(webDriver, () -> new Select(seleniumService.waitUntilElementPresent(webDriver, By.id("cboKlasse")))
                .getFirstSelectedOption().getText().equals(user.getUserGroup()));
        seleniumService.waitForJavascript(webDriver, 2000, 20);

        // Copy person email to role
        webDriver.get(PERSON_ROLE_URL);
        seleniumService.waitUntil(webDriver, ExpectedConditions.urlMatches(PERSON_ROLE_URL));

        seleniumService.click(webDriver, By.id("btnVonPers"));
        seleniumService.waitForJavascript(webDriver, 2000, 20);

        seleniumService.click(webDriver, By.id("btnSave"));
        seleniumService.waitForJavascript(webDriver, 2000, 20);

        webDriver.get(PERSON_EMAIL_URL);
        seleniumService.waitUntil(webDriver, ExpectedConditions.urlMatches(PERSON_EMAIL_URL));

        // Enter contact emails
        final String contactEmailsString = String.join(",", user.getContactEmails());
        seleniumService.clearAndSendKeys(webDriver, By.id("tbxLastschriftEMail"), contactEmailsString);
        seleniumService.clearAndSendKeys(webDriver, By.id("tbxHinweisEMail"), contactEmailsString);
        seleniumService.clearAndSendKeys(webDriver, By.id("tbxMitteilungen"), contactEmailsString);
        seleniumService.click(webDriver, By.id("btnSpeichern"));
        seleniumService.waitForJavascript(webDriver, 2000, 20);
    }

    private MensaMaxUser readUserData(final WebDriver webDriver) throws TimeoutException {
        seleniumService.waitUntil(webDriver, ExpectedConditions.or(ExpectedConditions.urlContains(PERSON_DATA_URL),
                ExpectedConditions.urlMatches(PERSON_CREATE_URL)));

        final String username = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxBenutzername")).getDomAttribute("value");
        final String firstName = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxVorname")).getDomAttribute("value");
        final String lastName = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxNachname")).getDomAttribute("value");
        final String dateOfBirth = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxGebDatum")).getDomAttribute("value");
        final String email = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxEmail")).getDomAttribute("value");
        final String userGroup = new Select(seleniumService.waitUntilElementPresent(webDriver, By.id("cboKlasse"))).getFirstSelectedOption().getText();
        int employeeId = 0;
        try {
            final String employeeIdString = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxPersNr")).getDomAttribute("value");
            if (employeeIdString != null && !employeeIdString.isEmpty()) {
                employeeId = Integer.parseInt(employeeIdString);
            }
        } catch (final NumberFormatException e) {
            log.warn("Invalid employeeId for user '{}'", username);
        }

        webDriver.get(PERSON_EMAIL_URL);
        seleniumService.waitUntil(webDriver, ExpectedConditions.urlMatches(PERSON_EMAIL_URL));

        final List<String> contactEmails = new ArrayList<>();
        final String contactEmailsString = seleniumService.waitUntilElementPresent(webDriver, By.id("tbxLastschriftEMail")).getText();
        if (!contactEmailsString.isEmpty()) {
            contactEmails.addAll(List.of(contactEmailsString.split(",")));
        }

        return new MensaMaxUser(username, firstName, lastName, email, contactEmails, dateOfBirth, userGroup, employeeId);
    }
}
