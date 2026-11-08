package ca.usherbrooke.ift605.mapek;

/**
 * Symptôme détecté par l'Analyzer : associe un service à un
 * {@link TypeSymptome}. L'objet est transmis au Planner pour décider d'une
 * action corrective.
 */
public class Symptome {
    /** Nom du service concerné. */
    public final String service;

    /** Type de symptôme détecté. */
    public final TypeSymptome type;

    /**
     * @param service nom du service concerné
     * @param type    type de symptôme détecté
     */
    public Symptome(String service, TypeSymptome type) {
        this.service = service;
        this.type = type;
    }
}