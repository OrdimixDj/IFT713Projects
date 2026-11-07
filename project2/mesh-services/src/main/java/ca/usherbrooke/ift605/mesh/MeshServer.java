package ca.usherbrooke.ift605.mesh;

import io.grpc.*;

public class MeshServer {
    // args[0] = calc | storage | notif
    public static void main(String[] args) throws Exception {
        String which = args.length > 0 ? args[0] : "calc";
        int port;
        BindableService svc;
        switch (which) {
            case "calc":
                port = 50051;
                svc = new CalcServiceImpl();
                break;
            case "storage":
                port = 50052;
                svc = new StorageServiceImpl();
                break;
            case "notif":
                port = 50053;
                svc = new NotifServiceImpl();
                break;
            default:
                throw new IllegalArgumentException("service inconnu: " + which);
        }

        Server server = ServerBuilder.forPort(port).addService(svc).build().start();
        System.out.println("[" + which + "] gRPC démarré sur le port " + port);
        server.awaitTermination();
    }
}