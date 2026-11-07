package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.*;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;

public class SondeLauncher {
    private static final int IPC = 9090; // port IPC utilisé

    public static void main(String[] args) {
        var calc = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        var sto = ManagedChannelBuilder.forAddress("localhost", 50052).usePlaintext().build();
        var noti = ManagedChannelBuilder.forAddress("localhost", 50053).usePlaintext().build();

        var cStub = CalcServiceGrpc.newBlockingStub(calc);
        var sStub = StorageServiceGrpc.newBlockingStub(sto);
        var nStub = NotifServiceGrpc.newBlockingStub(noti);
        HealthReq req = HealthReq.newBuilder().build();

        new Thread(new Sonde("CalcService", IPC,
                () -> cStub.withDeadlineAfter(2, TimeUnit.SECONDS).healthCheck(req))).start();
        new Thread(new Sonde("StorageService", IPC,
                () -> sStub.withDeadlineAfter(2, TimeUnit.SECONDS).healthCheck(req))).start();
        new Thread(new Sonde("NotifService", IPC,
                () -> nStub.withDeadlineAfter(2, TimeUnit.SECONDS).healthCheck(req))).start();

        System.out.println("Sondes démarrées (HealthCheck toutes les 5s au port " + IPC + ")");
    }
}