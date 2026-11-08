package ca.usherbrooke.ift605.mesh;

/**
 * Mesure de santé d'un service, produite par une {@link Sonde} puis transmise
 * au Monitor via un socket TCP, sérialisée en JSON.
 * Regroupe les six champs cités par l'énoncé : nom du service,
 * horodatage, statut, latence, charge CPU et uptime.
 */
public class MesureSante {
    /** Nom du service mesuré. */
    public String service;

    /** Horodatage de la mesure, temps epoch en millisecondes. */
    public long timestamp;

    /** Statut du service : {@code SERVING}, {@code DEGRADED} ou {@code DOWN}. */
    public String statut;

    /** Latence mesurée de l'appel HealthCheck, en millisecondes. */
    public long latenceMs;

    /** Charge CPU relevée par le service, entre 0.0 et 1.0. */
    public double cpuLoad;

    /**
     * Durée de fonctionnement du service depuis son démarrage, en millisecondes.
     */
    public long uptimeMs;
}