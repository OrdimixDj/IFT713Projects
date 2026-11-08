package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.*;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stocke et récupère des paires key/value en mémoire via une
 * {@link ConcurrentHashMap} thread-safe. Expose aussi un HealthCheck pour les
 * sondes.
 */
public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase {
    private final long startTime = System.currentTimeMillis();
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    /**
     * Stocke une valeur sous une clé.
     *
     * @param req  requête contenant la clé et la valeur
     * @param resp observateur de réponse (ok + valeur stockée)
     */
    @Override
    public void put(StoreRequest req, StreamObserver<StoreResponse> resp) {
        store.put(req.getKey(), req.getValue());
        resp.onNext(StoreResponse.newBuilder().setOk(true).setValue(req.getValue()).build());
        resp.onCompleted();
    }

    /**
     * Récupère la valeur associée à une clé.
     *
     * @param req  requête contenant la clé
     * @param resp observateur de réponse (ok=false si la clé est absente)
     */
    @Override
    public void get(GetRequest req, StreamObserver<StoreResponse> resp) {
        String v = store.get(req.getKey());
        resp.onNext(StoreResponse.newBuilder()
                .setOk(v != null)
                .setValue(v == null ? "" : v)
                .build());
        resp.onCompleted();
    }

    /**
     * Retourne l'état de santé du service pour les sondes.
     *
     * @param req  requête de health check
     * @param resp observateur de réponse ({@link HealthUtil#build})
     */
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