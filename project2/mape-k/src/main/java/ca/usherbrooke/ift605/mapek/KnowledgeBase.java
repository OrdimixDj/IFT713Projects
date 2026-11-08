package ca.usherbrooke.ift605.mapek;

import ca.usherbrooke.ift605.mesh.MesureSante;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Etat partagé entre Monitor, Analyzer et Executor. Conserve la dernière mesure
 * de chaque service, le compteur de pannes et la table de routage. Thread-safe
 * via {@link ConcurrentHashMap}.
 */
public class KnowledgeBase {
    private final ConcurrentHashMap<String, MesureSante> mesures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> nbPannes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> routage = new ConcurrentHashMap<>();

    /**
     * Enregistre une mesure ; une nouvelle mesure remplace l'état courant du
     * service.
     *
     * @param m mesure de santé reçue d'une sonde
     */
    public void majMesure(MesureSante m) {
        mesures.put(m.service, m);
    }

    /**
     * @return la table des dernières mesures, indexée par nom de service
     */
    public Map<String, MesureSante> getMesures() {
        return mesures;
    }

    /**
     * Incrémente le compteur de pannes d'un service.
     *
     * @param service nom du service
     */
    public void incrementerPannes(String service) {
        nbPannes.merge(service, 1, Integer::sum);
    }

    /**
     * @param service nom du service
     * @return le nombre de pannes enregistrées, 0 si aucune
     */
    public int getNbPannes(String service) {
        return nbPannes.getOrDefault(service, 0);
    }

    /**
     * Définit la cible de routage d'un service, pour le réacheminement de
     * l'Executor.
     *
     * @param service nom du service source
     * @param cible   nom de la cible
     */
    public void setRoute(String service, String cible) {
        routage.put(service, cible);
    }

    /**
     * @param service nom du service
     * @return la cible de routage, ou le service lui-même si aucune route n'est
     *         définie
     */
    public String getRoute(String service) {
        return routage.getOrDefault(service, service);
    }
}