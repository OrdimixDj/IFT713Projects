package ca.usherbrooke.ift605.mesh;

/**
 * Démarre une {@link Sonde} par service, chacune dans son propre thread, toutes
 * rapportant au Monitor sur le port IPC 9090.
 */
public class SondeLauncher {
    /** Port du Monitor vers lequel les sondes rapportent leurs mesures. */
    private static final int IPC = 9090;

    /**
     * Point d'entrée : démarre les trois sondes (Calc, Storage, Notif).
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {
        new Thread(new Sonde("CalcService", 50051, IPC)).start();
        new Thread(new Sonde("StorageService", 50052, IPC)).start();
        new Thread(new Sonde("NotifService", 50053, IPC)).start();
        System.out.println("Sondes démarrées (HealthCheck toutes les 5 s -> port " + IPC + ")");
    }
}