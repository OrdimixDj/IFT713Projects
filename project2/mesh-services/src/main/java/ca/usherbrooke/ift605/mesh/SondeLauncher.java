package ca.usherbrooke.ift605.mesh;

public class SondeLauncher {
    private static final int IPC = 9090;

    public static void main(String[] args) {
        new Thread(new Sonde("CalcService", 50051, IPC)).start();
        new Thread(new Sonde("StorageService", 50052, IPC)).start();
        new Thread(new Sonde("NotifService", 50053, IPC)).start();
        System.out.println("Sondes démarrées (HealthCheck toutes les 5 s -> port " + IPC + ")");
    }
}