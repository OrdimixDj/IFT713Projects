package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.*;
import io.grpc.*;

public class TestClient {
    public static void main(String[] args) {
        var calc = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        var sto = ManagedChannelBuilder.forAddress("localhost", 50052).usePlaintext().build();
        var noti = ManagedChannelBuilder.forAddress("localhost", 50053).usePlaintext().build();
        try {
            var c = CalcServiceGrpc.newBlockingStub(calc);
            System.out.println("Calc 3+4 = " + c.compute(
                    CalcRequest.newBuilder().setA(3).setB(4).setOp("+").build()).getResult());

            var s = StorageServiceGrpc.newBlockingStub(sto);
            s.put(StoreRequest.newBuilder().setKey("k").setValue("v").build());
            System.out.println("Storage get(k) = " + s.get(
                    GetRequest.newBuilder().setKey("k").build()).getValue());

            var n = NotifServiceGrpc.newBlockingStub(noti);
            System.out.println("Notif accepted = " + n.notify(
                    NotifRequest.newBuilder().setEvent("test").setPayload("hello").build()).getAccepted());
        } finally {
            calc.shutdown();
            sto.shutdown();
            noti.shutdown();
        }
    }
}