package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class Sonde implements Runnable {
    private final String service;
    private final int grpcPort; // port du service à surveiller
    private final int ipcPort; // port de rapport vers le gestionnaire
    private final ManagedChannel channel;
    private final ObjectMapper json = new ObjectMapper();

    public Sonde(String service, int grpcPort, int ipcPort) {
        this.service = service;
        this.grpcPort = grpcPort;
        this.ipcPort = ipcPort;
        this.channel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
                .usePlaintext().build(); // canal réutilisé à chaque cycle
    }

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

    // Appel gRPC HealthCheck -> MesureSante. Si l'appel échoue : mesure DOWN (pas
    // de crash).
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

    // Chaque service a son propre stub gRPC -> on choisit selon le nom du service
    // Yeld permet de retourner des données
    private HealthResp appelHealthCheck() {
        HealthReq req = HealthReq.newBuilder().setServiceName(service).build();
        return switch (service) {
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

    // Envoi de la mesure (JSON) au Monitor via socket TCP
    private void envoyerIPC(MesureSante m) {
        try (Socket s = new Socket("localhost", ipcPort);
                PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true)) {
            out.println(json.writeValueAsString(m));
        } catch (IOException e) {
            System.err.println("[Sonde " + service + "] Monitor injoignable: " + e.getMessage());
        }
    }
}