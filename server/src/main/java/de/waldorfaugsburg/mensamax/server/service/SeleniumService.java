package de.waldorfaugsburg.mensamax.server.service;

import de.waldorfaugsburg.mensamax.server.configuration.SeleniumConfigurationProperties;
import de.waldorfaugsburg.mensamax.server.selenium.SeleniumClient;
import de.waldorfaugsburg.mensamax.server.selenium.SeleniumClientStack;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverLogLevel;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@Slf4j
public class SeleniumService {

    private static final ExpectedCondition<Boolean> WAIT_CONDITION = driver -> ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");

    private final Set<SeleniumClientStack> stacks = new HashSet<>();
    private final SeleniumConfigurationProperties properties;
    private int instanceId;

    public SeleniumService(final SeleniumConfigurationProperties properties) {
        System.setProperty("webdriver.chrome.driver", properties.driverPath());
        this.properties = properties;
    }

    @PreDestroy
    public void shutdown() throws IOException {
        for (final SeleniumClientStack stack : stacks) {
            stack.close();
        }
    }

    public void clearAndSendKeys(final WebDriver webDriver, final String elementId, final CharSequence... keysToSend) {
        final Supplier<WebElement> elementSupplier = () -> webDriver.findElement(By.id(elementId));
        elementSupplier.get().clear();
        for (final CharSequence sequence : keysToSend) {
            elementSupplier.get().sendKeys(sequence);
        }
    }

    public void waitUntilPageReady(final WebDriver webDriver) {
        final WebDriverWait driverWait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        driverWait.until(WAIT_CONDITION);
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

    private ChromeDriver createDriverInstance() {
        final File userDir = new File(properties.profilePath() + instanceId);
        final File logPath = new File(userDir, "latest.log");
        try {
            Files.createDirectories(userDir.toPath());
            if (!logPath.exists()) {
                Files.createFile(logPath.toPath());
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        options.addArguments("--user-data-dir=" + userDir.getAbsolutePath(), "--no-sandbox");
        options.setLogLevel(ChromeDriverLogLevel.ALL);
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        System.setProperty("webdriver.chrome.logfile", logPath.getAbsolutePath());

        instanceId++;
        return new ChromeDriver(options);
    }
}
