package org.example;

import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RpcDatabaseServerTest {

    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    public void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(serverName)
                .addService(new DatabaseServiceImpl())
                .directExecutor()
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
    }

    @AfterEach
    public void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    public void testServerStartsSuccessfully() {
        assertNotNull(server, "Server should have been initialized.");
    }

    // Optional: Weitere Tests mit gRPC-Stubs, wenn verfügbar.
}
