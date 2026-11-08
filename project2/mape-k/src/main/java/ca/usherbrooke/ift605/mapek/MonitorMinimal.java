package ca.usherbrooke.ift605.mapek;

import java.io.*;
import java.net.*;

/**
 * Version minimale du Monitor, qui respecte les consignes de l'énoncé pour la
 * S1 : serveur IPC qui se contente d'afficher les mesures reçues des sondes,
 * sans KnowledgeBase ni analyse. Le moniteur a été remplacé par {@link Monitor}
 * dans la boucle MAPE-K.
 */
public class MonitorMinimal {
    /**
     * Point d'entrée : écoute sur le port IPC 9090 et affiche chaque ligne reçue.
     *
     * @param args non utilisés
     * @throws IOException si le socket serveur ne peut pas être ouvert
     */
    public static void main(String[] args) throws IOException {
        int port = 9090;
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[Monitor] est en écoute sur le port " + port);
            while (true) {
                Socket s = server.accept();
                new Thread(() -> {
                    // in va être automatiquement fermé à la fin du bloc try
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                        String line;
                        while ((line = in.readLine()) != null)
                            System.out.println("[Monitor] reçu: " + line);
                    } catch (IOException ignored) {
                    }
                }).start();
            }
        }
    }
}