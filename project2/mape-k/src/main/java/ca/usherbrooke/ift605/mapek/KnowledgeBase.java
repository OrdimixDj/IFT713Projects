package ca.usherbrooke.ift605.mapek;

import ca.usherbrooke.ift605.mesh.MesureSante;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KnowledgeBase {
    private final ConcurrentHashMap<String, MesureSante> mesures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> nbPannes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> routage = new ConcurrentHashMap<>();

    // une nouvelle mesure remplace l'état courant du service
    public void majMesure(MesureSante m) {
        mesures.put(m.service, m);
    }

    public Map<String, MesureSante> getMesures() {
        return mesures;
    }

    // compteur de pannes par service
    public void incrementerPannes(String service) {
        nbPannes.merge(service, 1, Integer::sum);
    }

    public int getNbPannes(String service) {
        return nbPannes.getOrDefault(service, 0);
    }

    // table de routage, pour le réacheminement de l'Executor
    public void setRoute(String service, String cible) {
        routage.put(service, cible);
    }

    public String getRoute(String service) {
        return routage.getOrDefault(service, service);
    }
}