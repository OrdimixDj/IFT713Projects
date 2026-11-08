package ca.usherbrooke.ift605.chaos;

import ca.usherbrooke.ift605.mapek.GestionnaireAutonome;
import ca.usherbrooke.ift605.mapek.KnowledgeBase;
import ca.usherbrooke.ift605.mesh.MesureSante;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChaosMonkey {
    private final KnowledgeBase kb;
    private final Map<String, Integer> ports;

    public ChaosMonkey(KnowledgeBase kb, Map<String, Integer> ports) {
        this.kb = kb;
        this.ports = ports;
    }

    // Tue un service aléatoire toutes les 30 secondes
    public void demarrer(List<String> services) {
        ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
        sched.scheduleAtFixedRate(() -> {
            String cible = services.get(new Random().nextInt(services.size()));
            System.out.println("[Chaos] Arrêt de " + cible);
            // Envoyer SIGTERM au processus gRPC correspondant
            long tPanne = System.currentTimeMillis();
            tuerParPort(ports.get(cible));
            // Mesurer le temps de récupération automatique par le Gestionnaire
            new Thread(() -> mesurer(cible, tPanne)).start();
        }, 30, 30, TimeUnit.SECONDS);
    }

    private void tuerParPort(int port) {
        try {
            new ProcessBuilder("bash", "-c", "lsof -ti tcp:" + port + " -sTCP:LISTEN | xargs kill")
                    .inheritIO().start().waitFor();
        } catch (Exception e) {
            System.err.println("[Chaos] echec kill port " + port + " : " + e.getMessage());
        }
    }

    private void mesurer(String service, long tPanne) {
        long tDetection = 0, tRecup = 0;
        while (tRecup == 0) {
            MesureSante m = kb.getMesures().get(service);
            if (m != null) {
                if (tDetection == 0 && "DOWN".equals(m.statut))
                    tDetection = System.currentTimeMillis();
                if (tDetection != 0 && "SERVING".equals(m.statut))
                    tRecup = System.currentTimeMillis();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
        }
        System.out.printf("[Chaos] %s | detection=+%dms | recuperation=+%dms | MTTR=%dms%n",
                service, tDetection - tPanne, tRecup - tPanne, tRecup - tDetection);
    }

    public static void main(String[] args) {
        GestionnaireAutonome gestionnaire = new GestionnaireAutonome();
        gestionnaire.demarrer();

        Map<String, Integer> ports = Map.of(
                "CalcService", 50051,
                "StorageService", 50052,
                "NotifService", 50053);
        new ChaosMonkey(gestionnaire.getKb(), ports)
                .demarrer(List.of("CalcService", "StorageService", "NotifService"));
        System.out.println("[Chaos] ChaosMonkey démarré (panne programmée toutes les 30 s)");
    }
}