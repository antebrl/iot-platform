package org.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class RpcDatabaseServer {

    public static void main(String[] args) throws Exception {
        int port = 50051;
        Server server = ServerBuilder.forPort(port)
                .addService(new DatabaseServiceImpl())
                .build()
                .start();

        System.out.printf("RPC Database Server started on port %d%n", port);
        server.awaitTermination();
    }
}
