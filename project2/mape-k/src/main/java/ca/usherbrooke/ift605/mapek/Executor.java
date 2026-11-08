package ca.usherbrooke.ift605.mapek;

import java.io.IOException;

public class Executor {
    private final KnowledgeBase kb;
    private final java.util.Map<String, Long> dernierRedemarrage = new java.util.concurrent.ConcurrentHashMap<>();

    public Executor(KnowledgeBase kb) {
        this.kb = kb;
    }

    public void executer(ActionCorrective action) {
        switch (action.type) {
            case REDEMARRER -> redemarrer(action.service);
            case REACHEMINEMENT -> reacheminer(action.service);
            case MISE_A_ECHELLE -> System.out.println("[Execute] MISE_A_ECHELLE " + action.service);
        }
    }

    private void redemarrer(String service) {
        // pas de redémarrage si le service a été redémarré il y a moins de 30 secondes
        long now = System.currentTimeMillis();
        Long last = dernierRedemarrage.get(service);
        if (last != null && now - last < 30000)
            return;
        dernierRedemarrage.put(service, now);
        kb.incrementerPannes(service);
        System.out.println("[Execute] REDEMARRER " + service);
        try {
            new ProcessBuilder("mvn", "-q", "-pl", "mesh-services", "exec:java",
                    "-Dexec.mainClass=ca.usherbrooke.ift605.mesh.MeshServer",
                    "-Dexec.args=" + argModule(service))
                    .inheritIO().start();
        } catch (IOException e) {
            System.err.println("[Execute] echec redemarrage " + service + " : " + e.getMessage());
        }
    }

    private void reacheminer(String service) {
        kb.setRoute(service, service + "_backup");
        System.out.println("[Execute] REACHEMINEMENT " + service + " -> " + kb.getRoute(service));
    }

    private String argModule(String service) {
        return switch (service) {
            case "CalcService" -> "calc";
            case "StorageService" -> "storage";
            case "NotifService" -> "notif";
            default -> throw new IllegalArgumentException("Service inconnu: " + service);
        };
    }
}