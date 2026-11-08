package ca.usherbrooke.ift605.mapek;

import ca.usherbrooke.ift605.mesh.MesureSante;
import java.util.ArrayList;
import java.util.List;

/**
 * Parcourt les mesures de la {@link KnowledgeBase} et en déduit des symptômes.
 * Les règles sont : DOWN -> PANNE_TOTALE, latence > 500 ms -> DEGRADATION,
 * charge CPU > 0.90 -> SURCHARGE.
 */
public class Analyzer {
    private final KnowledgeBase kb;

    /**
     * @param kb base de connaissances à analyser
     */
    public Analyzer(KnowledgeBase kb) {
        this.kb = kb;
    }

    /**
     * Analyse l'état courant de tous les services.
     *
     * @return la liste des symptômes détectés, vide si tout est sain
     */
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