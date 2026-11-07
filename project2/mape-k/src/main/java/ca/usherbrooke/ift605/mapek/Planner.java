package ca.usherbrooke.ift605.mapek;

import java.util.List;

public class Planner {
    public List<ActionCorrective> planifier(List<Symptome> symptomes) {
        return symptomes.stream().map(s -> switch (s.type) {
            case PANNE_TOTALE -> new ActionCorrective(s.service, TypeAction.REDEMARRER);
            case DEGRADATION -> new ActionCorrective(s.service, TypeAction.REACHEMINEMENT);
            case SURCHARGE -> new ActionCorrective(s.service, TypeAction.MISE_A_ECHELLE);
        }).toList();
    }
}