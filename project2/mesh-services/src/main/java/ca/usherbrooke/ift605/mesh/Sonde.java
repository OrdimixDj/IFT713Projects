package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Sonde de surveillance d'un service gRPC.
 *
 * Tourne dans son propre thread : toutes les 5 secondes, elle interroge le
 * HealthCheck du service surveillé, en déduit une {@link MesureSante}, puis
 * l'envoie au Monitor via un socket TCP au format JSON. Si l'appel gRPC échoue,
 * elle produit une mesure {@code DOWN} au lieu de s'arrêter.
 */
public class Sonde implements Runnable {
    /** Nom du service surveillé. */
    private final String service;

    /** Port gRPC du service à surveiller. */
    private final int grpcPort;

    /** Port du Monitor vers lequel la mesure est rapportée. */
    private final int ipcPort;

    /** Canal gRPC réutilisé à chaque cycle. */
    private final ManagedChannel channel;

    /** Sérialiseur JSON des mesures. */
    private final ObjectMapper json = new ObjectMapper();

    /**
     * @param service  nom du service surveillé
     * @param grpcPort port gRPC du service
     * @param ipcPort  port du Monitor, pour le rapport des ressources
     */
    public Sonde(String service, int grpcPort, int ipcPort) {
        this.service = service;
        this.grpcPort = grpcPort;
        this.ipcPort = ipcPort;
        this.channel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
                .usePlaintext().build(); // canal réutilisé à chaque cycle
    }

    /** Boucle de surveillance : HealthCheck -> rapport IPC -> pause de 5s. */
    @Override
    public void run() {
        while (true) {
            MesureSante m = pinger(); // appel gRPC healthCheck
            envoyerIPC(m); // rapport via socket
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Interroge le HealthCheck du service et construit la mesure correspondante.
     * En cas d'échec de l'appel gRPC, renvoie une mesure de statut {@code DOWN}.
     *
     * @return la mesure de santé du service
     */
    private MesureSante pinger() {
        MesureSante m = new MesureSante();
        m.service = service;
        m.timestamp = System.currentTimeMillis();
        long t0 = System.currentTimeMillis();
        try {
            HealthResp r = appelHealthCheck();
            m.latenceMs = System.currentTimeMillis() - t0;
            m.statut = r.getStatus();
            m.cpuLoad = r.getCpuLoad();
            m.uptimeMs = r.getUptimeMs();
        } catch (Exception e) {
            m.latenceMs = System.currentTimeMillis() - t0;
            m.statut = "DOWN";
            m.cpuLoad = 0;
            m.uptimeMs = 0;
        }
        return m;
    }

    /**
     * Appelle le HealthCheck via le bon stub gRPC selon le service surveillé
     * (chaque service a son propre stub généré).
     * 
     * @return la réponse de HealthCheck
     */
    private HealthResp appelHealthCheck() {
        HealthReq req = HealthReq.newBuilder().setServiceName(service).build();
        return switch (service) { // Yeld permet de retourner des données
            case "CalcService":
                yield CalcServiceGrpc.newBlockingStub(channel)
                        .withDeadlineAfter(2, TimeUnit.SECONDS).healthCheck(req);
            case "StorageService":
                yield StorageServiceGrpc.newBlockingStub(channel)
                        .withDeadlineAfter(2, TimeUnit.SECONDS).healthCheck(req);
            case "NotifService":
                yield NotifServiceGrpc.newBlockingStub(channel)
                        .withDeadlineAfter(2, TimeUnit.SECONDS).healthCheck(req);
            default:
                throw new IllegalArgumentException("Service inconnu: " + service);
        };
    }

    /**
     * Envoie la mesure au Monitor via un socket TCP (une ligne en JSON).
     *
     * @param m la mesure à transmettre
     */
    private void envoyerIPC(MesureSante m) {
        try (Socket s = new Socket("localhost", ipcPort);
                PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true)) {
            out.println(json.writeValueAsString(m));
        } catch (IOException e) {
            System.err.println("[Sonde " + service + "] Monitor injoignable: " + e.getMessage());
        }
    }
}