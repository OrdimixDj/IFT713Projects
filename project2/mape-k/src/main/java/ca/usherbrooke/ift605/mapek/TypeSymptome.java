package ca.usherbrooke.ift605.mapek;

/**
 * Types de symptômes détectés par l'Analyzer à partir des mesures de santé :
 * panne totale (service DOWN), dégradation (latence élevée) ou surcharge
 * (charge CPU élevée).
 */
public enum TypeSymptome {
    PANNE_TOTALE, DEGRADATION, SURCHARGE
}