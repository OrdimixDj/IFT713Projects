package ca.usherbrooke.ift605.mapek;

import java.io.*;
import java.net.*;

public class MonitorMinimal {
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