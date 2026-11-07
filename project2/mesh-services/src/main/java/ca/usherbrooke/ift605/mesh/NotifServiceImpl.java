package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.*;
import io.grpc.stub.StreamObserver;

public class NotifServiceImpl extends NotifServiceGrpc.NotifServiceImplBase {
    private final long startTime = System.currentTimeMillis();

    @Override
    public void notify(NotifRequest req, StreamObserver<NotifResponse> resp) {
        System.out.println("[Notif] event=" + req.getEvent() + " payload=" + req.getPayload());
        resp.onNext(NotifResponse.newBuilder().setAccepted(true).build());
        resp.onCompleted();
    }

    @Override
    public void healthCheck(HealthReq req, StreamObserver<HealthResp> resp) {
        resp.onNext(HealthUtil.build(startTime));
        resp.onCompleted();
    }
}