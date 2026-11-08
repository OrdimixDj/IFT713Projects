package ca.usherbrooke.ift605.mesh;

import io.grpc.*;

/**
 * Lanceur générique d'un micro-service gRPC.
 *
 * Démarre, dans un processus Java indépendant, l'un des trois services sur son
 * port dédié selon l'argument passé : {@code calc} (50051), {@code storage}
 * (50052) ou {@code notif} (50053).
 */
public class MeshServer {
    /**
     * Point d'entrée : démarre le service demandé et bloque jusqu'à son arrêt.
     *
     * @param args {@code args[0]} = {@code calc}, {@code storage} ou {@code notif}
     *             (valeur par défaut : {@code calc})
     * @throws Exception si le serveur gRPC ne peut pas démarrer ou est interrompu
     */
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