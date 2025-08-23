package de.waldorfaugsburg.mensamax.server.selenium;

import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.Closeable;

@Getter
public final class SeleniumClient implements Closeable {

    private final int instanceId;
    private final FirefoxDriver webDriver;

    @Setter
    private long lastActionDate;
    @Setter
    private String currentKiosk;

    public SeleniumClient(final int instanceId, final FirefoxDriver webDriver) {
        this.instanceId = instanceId;
        this.webDriver = webDriver;
    }

    @Override
    public void close() {
        webDriver.quit();
    }
}
