package ca.usherbrooke.ift605.mapek;

import java.io.IOException;

/**
 * Exécute les actions correctives planifiées : redémarre un service (via mvn
 * exec:java), le réachemine vers un backup, ou applique une mise à l'échelle.
 * Une sécurité empêche de redémarrer deux fois le même service en moins de 30
 * secondes, pour éviter une erreur en cas de double démarrage sur le même port.
 */
public class Executor {
    private final KnowledgeBase kb;
    private final java.util.Map<String, Long> dernierRedemarrage = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * @param kb base de connaissances (KnowledgeBase) qui contient le compteur de
     *           pannes et la table de routage
     */
    public Executor(KnowledgeBase kb) {
        this.kb = kb;
    }

    /**
     * Exécute une action corrective selon son type.
     *
     * @param action action à exécuter
     */
    public void executer(ActionCorrective action) {
        switch (action.type) {
            case REDEMARRER -> redemarrer(action.service);
            case REACHEMINEMENT -> reacheminer(action.service);
            case MISE_A_ECHELLE -> System.out.println("[Execute] MISE_A_ECHELLE " + action.service);
        }
    }

    /**
     * Redémarre un service via mvn exec:java, sauf s'il a déjà été redémarré il y
     * a moins de 30 secondes. Incrémente le compteur de pannes dans la
     * KnowledgeBase.
     *
     * @param service nom du service à redémarrer
     */
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

    /**
     * Réachemine un service vers son backup en mettant à jour la table de routage.
     *
     * @param service nom du service à réacheminer
     */
    private void reacheminer(String service) {
        kb.setRoute(service, service + "_backup");
        System.out.println("[Execute] REACHEMINEMENT " + service + " -> " + kb.getRoute(service));
    }

    /**
     * Traduit un nom de service en argument de module pour MeshServer.
     *
     * @param service nom du service
     * @return l'argument module ("calc", "storage" ou "notif")
     * @throws IllegalArgumentException si le service est inconnu
     */
    private String argModule(String service) {
        return switch (service) {
            case "CalcService" -> "calc";
            case "StorageService" -> "storage";
            case "NotifService" -> "notif";
            default -> throw new IllegalArgumentException("Service inconnu: " + service);
        };
    }
}