package de.waldorfaugsburg.mensamax.server.service;

import de.waldorfaugsburg.mensamax.server.configuration.SeleniumConfigurationProperties;
import de.waldorfaugsburg.mensamax.server.selenium.SeleniumClient;
import de.waldorfaugsburg.mensamax.server.selenium.SeleniumClientStack;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@Slf4j
public class SeleniumService {

    private final Set<SeleniumClientStack> stacks = new HashSet<>();
    private final SeleniumConfigurationProperties properties;
    private int instanceId;

    public SeleniumService(final SeleniumConfigurationProperties properties) {
        System.setProperty("webdriver.gecko.driver", properties.driverPath());
        this.properties = properties;
    }

    @PreDestroy
    public void shutdown() throws IOException {
        for (final SeleniumClientStack stack : stacks) {
            stack.close();
        }
    }

    public void click(final WebDriver webDriver, final By locator) throws TimeoutException {
        final WebElement element = waitUntilElementPresent(webDriver, locator);
        element.click();
    }

    public void clearAndSendKeys(final WebDriver webDriver, final By locator, final CharSequence... keysToSend) throws TimeoutException {
        final WebElement element = waitUntilElementPresent(webDriver, locator);
        element.clear();
        for (final CharSequence sequence : keysToSend) {
            element.sendKeys(sequence);
        }
    }

    public WebElement waitUntilElementPresent(final WebDriver webDriver, final By locator) throws TimeoutException {
        return waitUntil(webDriver, ExpectedConditions.refreshed(ExpectedConditions.presenceOfElementLocated(locator)));
    }

    public List<WebElement> waitUntilElementsPresent(final WebDriver webDriver, final By locator) throws TimeoutException {
        return waitUntil(webDriver, ExpectedConditions.refreshed(ExpectedConditions.presenceOfAllElementsLocatedBy(locator)));
    }

    public <T> T waitUntil(final WebDriver webDriver, final Function<? super WebDriver, T> condition) throws TimeoutException {
        final WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(1));
        return wait.until(condition);
    }

    public SeleniumClientStack reserveClients(final int amount, final Consumer<SeleniumClient> initialize) {
        final BlockingQueue<SeleniumClient> clients = new LinkedBlockingQueue<>(amount);

        for (int i = 0; i < amount; i++) {
            final SeleniumClient client = new SeleniumClient(instanceId, createDriverInstance());
            initialize.accept(client);
            clients.add(client);
        }

        final SeleniumClientStack stack = new SeleniumClientStack(clients);
        stacks.add(stack);
        return stack;
    }

    private FirefoxDriver createDriverInstance() {
        final File userDir = new File(properties.profilePath() + instanceId);
        try {
            Files.createDirectories(userDir.toPath());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final FirefoxOptions options = new FirefoxOptions();
        options.addArguments("-profile", userDir.getAbsolutePath());
        options.setHeadless(true);

        instanceId++;
        FirefoxDriver firefoxDriver = new FirefoxDriver(options);
        firefoxDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5));
        return firefoxDriver;
    }
}
