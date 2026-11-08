package ca.usherbrooke.ift605.mapek;

/**
 * Types d'actions correctives choisies par le Planner en réponse à un symptôme
 * : redémarrer le service, réacheminer vers un backup, ou mettre à l'échelle.
 */
public enum TypeAction {
    REDEMARRER, REACHEMINEMENT, MISE_A_ECHELLE
}