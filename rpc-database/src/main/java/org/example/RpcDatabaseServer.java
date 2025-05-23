package org.example;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class RpcDatabaseServer {
    private Server server;

    public void start(int port) throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new DatabaseServiceImpl())
                .build()
                .start();
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        RpcDatabaseServer s = new RpcDatabaseServer();
        s.start(50051);
        System.out.println("Server started.");
        s.blockUntilShutdown();
    }
}
