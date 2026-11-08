package ca.usherbrooke.ift605.mapek;

/**
 * Associe un service à un {@link TypeAction}. Objet produit par le Planner et
 * exécuté par l'Executor.
 */
public class ActionCorrective {
    /** Nom du service ciblé par l'action. */
    public final String service;

    /** Type d'action corrective à exécuter. */
    public final TypeAction type;

    /**
     * @param service nom du service ciblé
     * @param type    type d'action à exécuter
     */
    public ActionCorrective(String service, TypeAction type) {
        this.service = service;
        this.type = type;
    }
}