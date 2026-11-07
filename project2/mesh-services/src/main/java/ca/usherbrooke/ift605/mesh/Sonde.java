package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.HealthResp;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;

public class Sonde implements Runnable {
    @FunctionalInterface
    public interface HealthProbe {
        HealthResp check() throws Exception;
    }

    private final String service;
    private final int ipcPort; // port du Monitor (9090)
    private final HealthProbe probe; // appel gRPC healthCheck du service surveillé
    private final ObjectMapper json = new ObjectMapper();

    public Sonde(String service, int ipcPort, HealthProbe probe) {
        this.service = service;
        this.ipcPort = ipcPort;
        this.probe = probe;
    }

    @Override
    public void run() {
        while (true) {
            MesureSante m = new MesureSante();
            m.service = service;
            m.timestamp = System.currentTimeMillis();
            long t0 = System.currentTimeMillis();
            try {
                HealthResp r = probe.check(); // essai de l'appel gRPC
                m.latenceMs = System.currentTimeMillis() - t0;
                m.statut = r.getStatus();
                m.cpuLoad = r.getCpuLoad();
                m.uptimeMs = r.getUptimeMs();
            } catch (Exception e) { // cas où le service est injoignable
                m.latenceMs = System.currentTimeMillis() - t0;
                m.statut = "DOWN";
                m.cpuLoad = 0;
                m.uptimeMs = 0;
            }
            envoyerIPC(m);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void envoyerIPC(MesureSante m) {
        try (Socket s = new Socket("localhost", ipcPort);
                PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true)) {
            out.println(json.writeValueAsString(m)); // on print une ligne JSON par mesure
        } catch (IOException e) {
            System.err.println("[Sonde " + service + "] Monitor injoignable: " + e.getMessage());
        }
    }
}