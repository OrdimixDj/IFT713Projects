package ca.usherbrooke.ift605.mapek;

import java.util.List;

/**
 * Traduit chaque symptôme en action corrective. PANNE_TOTALE -> REDEMARRER,
 * DEGRADATION -> REACHEMINEMENT, SURCHARGE -> MISE_A_ECHELLE.
 */
public class Planner {
    /**
     * Planifie les actions correctives pour une liste de symptômes.
     *
     * @param symptomes symptômes détectés par l'Analyzer
     * @return la liste des actions correctives correspondantes
     */
    public List<ActionCorrective> planifier(List<Symptome> symptomes) {
        return symptomes.stream().map(s -> switch (s.type) {
            case PANNE_TOTALE -> new ActionCorrective(s.service, TypeAction.REDEMARRER);
            case DEGRADATION -> new ActionCorrective(s.service, TypeAction.REACHEMINEMENT);
            case SURCHARGE -> new ActionCorrective(s.service, TypeAction.MISE_A_ECHELLE);
        }).toList();
    }
}