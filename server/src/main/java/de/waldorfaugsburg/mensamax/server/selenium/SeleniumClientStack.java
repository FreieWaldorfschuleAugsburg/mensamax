package de.waldorfaugsburg.mensamax.server.selenium;

import de.waldorfaugsburg.mensamax.server.exception.NoClientsAvailableException;
import de.waldorfaugsburg.mensamax.server.exception.UnknownErrorException;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public record SeleniumClientStack(BlockingQueue<SeleniumClient> clients) implements Closeable {

    public SeleniumClient obtainClient() {
        try {
            final SeleniumClient client = clients.poll(5, TimeUnit.SECONDS);
            if (client == null) {
                log.error("Error obtaining client: no clients available!");
                throw new NoClientsAvailableException();
            }

            log.info("Client '{}' obtained!", client.getInstanceId());
            return client;
        } catch (final InterruptedException e) {
            log.error("Error obtaining a client", e);
            throw new UnknownErrorException(e);
        }
    }

    public void returnClient(final SeleniumClient client) {
        clients.add(client);
        log.info("Client '{}' returned!", client.getInstanceId());
    }

    @Override
    public void close() throws IOException {
        for (final SeleniumClient client : clients) {
            client.close();
        }
    }
}
