package ca.usherbrooke.ift605.mapek;

import ca.usherbrooke.ift605.mesh.MesureSante;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Serveur IPC qui reçoit les mesures de santé envoyées par les sondes (JSON sur
 * socket TCP) et les stocke dans la {@link KnowledgeBase}. Une mesure invalide
 * est ignorée.
 */
public class Monitor {
    private final KnowledgeBase kb;
    private final ObjectMapper json = new ObjectMapper();
    private ServerSocket server;

    /**
     * @param kb base de connaissances (KnowledgeBase) où déposer les mesures reçues
     */
    public Monitor(KnowledgeBase kb) {
        this.kb = kb;
    }

    /**
     * Démarre l'écoute IPC : reçoit les mesures des sondes et les stocke dans la
     * KnowledgeBase. Chaque connexion est traitée dans son propre thread.
     *
     * @param port port d'écoute IPC
     * @throws IOException si le socket serveur ne peut pas être ouvert
     */
    public void demarrer(int port) throws IOException {
        server = new ServerSocket(port);
        System.out.println("[Monitor] en écoute sur le port " + port);
        while (true) {
            Socket s = server.accept();
            new Thread(() -> traiter(s)).start();
        }
    }

    private void traiter(Socket s) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                MesureSante m = json.readValue(line, MesureSante.class); // parse en JSON
                if (m.service == null || m.statut == null) {
                    continue; // validation minimale : on ignore une mesure invalide
                }
                kb.majMesure(m); // met à jour l'état courant dans la KnowledgeBase
                System.out.println("[Monitor] mise à jour de la KnowledgeBase: " + m.service + " -> " + m.statut
                        + " (lat=" + m.latenceMs + "ms, cpu=" + m.cpuLoad + ")");
            }
        } catch (IOException e) {
            System.err.println("[Monitor] erreur lecture: " + e.getMessage());
        }
    }
}