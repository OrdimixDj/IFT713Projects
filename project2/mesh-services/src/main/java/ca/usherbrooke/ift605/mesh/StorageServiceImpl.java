package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.*;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.ConcurrentHashMap;

public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase {
    private final long startTime = System.currentTimeMillis();
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    @Override
    public void put(StoreRequest req, StreamObserver<StoreResponse> resp) {
        store.put(req.getKey(), req.getValue());
        resp.onNext(StoreResponse.newBuilder().setOk(true).setValue(req.getValue()).build());
        resp.onCompleted();
    }

    @Override
    public void get(GetRequest req, StreamObserver<StoreResponse> resp) {
        String v = store.get(req.getKey());
        resp.onNext(StoreResponse.newBuilder()
                .setOk(v != null)
                .setValue(v == null ? "" : v)
                .build());
        resp.onCompleted();
    }

    @Override
    public void healthCheck(HealthReq req, StreamObserver<HealthResp> resp) {
        /*
         * A UTILISER POUR LA DEMO: SIMULE UNE LATENCE
         * try {
         * Thread.sleep(600);
         * } catch (InterruptedException e) {
         * }
         */
        resp.onNext(HealthUtil.build(startTime));
        resp.onCompleted();
    }
}