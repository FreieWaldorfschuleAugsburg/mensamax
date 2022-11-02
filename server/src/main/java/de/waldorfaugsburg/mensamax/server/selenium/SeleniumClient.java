package de.waldorfaugsburg.mensamax.server.selenium;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.Closeable;
import java.io.IOException;

public final class SeleniumClient implements Closeable {

    private final int instanceId;
    private final ChromeDriver webDriver;

    private long lastActionDate;
    private String currentKiosk;

    public SeleniumClient(final int instanceId, final ChromeDriver webDriver) {
        this.instanceId = instanceId;
        this.webDriver = webDriver;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public ChromeDriver getWebDriver() {
        return webDriver;
    }

    public String getCurrentKiosk() {
        return currentKiosk;
    }

    public void setCurrentKiosk(final String currentKiosk) {
        this.currentKiosk = currentKiosk;
    }

    public long getLastActionDate() {
        return lastActionDate;
    }

    public void setLastActionDate(final long lastActionDate) {
        this.lastActionDate = lastActionDate;
    }

    @Override
    public void close() {
        webDriver.quit();
    }
}
