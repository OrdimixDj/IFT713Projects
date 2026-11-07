package ca.usherbrooke.ift605.mapek;

import ca.usherbrooke.ift605.mesh.MesureSante;
import java.util.ArrayList;
import java.util.List;

public class Analyzer {
    private final KnowledgeBase kb;

    public Analyzer(KnowledgeBase kb) {
        this.kb = kb;
    }

    // Retourne une liste de symptômes détectés
    public List<Symptome> analyser() {
        List<Symptome> symptomes = new ArrayList<>();
        for (var entry : kb.getMesures().entrySet()) {
            MesureSante m = entry.getValue();
            if ("DOWN".equals(m.statut)) {
                symptomes.add(new Symptome(entry.getKey(), TypeSymptome.PANNE_TOTALE));
            } else if (m.latenceMs > 500) {
                symptomes.add(new Symptome(entry.getKey(), TypeSymptome.DEGRADATION));
            } else if (m.cpuLoad > 0.90) {
                symptomes.add(new Symptome(entry.getKey(), TypeSymptome.SURCHARGE));
            }
        }
        return symptomes;
    }
}