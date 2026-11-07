package ca.usherbrooke.ift605.mesh;

public class MesureSante {
    public String service;
    public long timestamp;
    public String statut; // SERVING | DEGRADED | DOWN
    public long latenceMs;
    public double cpuLoad;
    public long uptimeMs;
}