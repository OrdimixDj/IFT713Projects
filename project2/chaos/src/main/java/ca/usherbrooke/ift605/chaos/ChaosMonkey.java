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

/**
 * Injecte des pannes pour valider l'auto-réparation : tue un service aléatoire
 * toutes les 30 secondes, puis observe la {@link KnowledgeBase} pour mesurer le
 * temps de détection et de récupération (MTTR) assurés par le gestionnaire
 * autonome.
 */
public class ChaosMonkey {
    private final KnowledgeBase kb;
    private final Map<String, Integer> ports;

    /**
     * @param kb    base de connaissances (KnowledgeBase) où lire l'état des
     *              services
     * @param ports table qui associe chaque service à son port gRPC
     */
    public ChaosMonkey(KnowledgeBase kb, Map<String, Integer> ports) {
        this.kb = kb;
        this.ports = ports;
    }

    /**
     * Tue un service aléatoire toutes les 30 secondes et lance la mesure du MTTR.
     *
     * @param services liste des services qui peuvent tomber en panne
     */
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

    /**
     * Tue le processus qui écoute sur un port donné (uniquement le serveur en
     * écoute, pas les clients connectés).
     *
     * @param port port gRPC du service à arrêter
     */
    private void tuerParPort(int port) {
        try {
            new ProcessBuilder("bash", "-c", "lsof -ti tcp:" + port + " -sTCP:LISTEN | xargs kill")
                    .inheritIO().start().waitFor();
        } catch (Exception e) {
            System.err.println("[Chaos] echec kill port " + port + " : " + e.getMessage());
        }
    }

    /**
     * Observe la KnowledgeBase jusqu'au retour normal SERVING du service, puis
     * affiche le temps de détection, de récupération et le MTTR.
     *
     * @param service nom du service surveillé
     * @param tPanne  instant, en ms, de l'injection de la panne
     */
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

    /**
     * Point d'entrée : démarre le gestionnaire autonome puis le ChaosMonkey.
     *
     * @param args non utilisés
     */
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