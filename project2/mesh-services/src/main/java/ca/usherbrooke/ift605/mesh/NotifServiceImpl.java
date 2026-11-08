package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.HealthReq;
import ca.usherbrooke.ift605.mesh.proto.HealthResp;
import ca.usherbrooke.ift605.mesh.proto.NotifRequest;
import ca.usherbrooke.ift605.mesh.proto.NotifResponse;
import ca.usherbrooke.ift605.mesh.proto.NotifServiceGrpc;
import io.grpc.stub.StreamObserver;

/**
 * Implémentation gRPC du service de notification.
 *
 * Reçoit un événement via {@code notify} et confirme sa prise en charge, et
 * expose un {@code healthCheck}, délégué à {@link HealthUtil}.
 */
public class NotifServiceImpl extends NotifServiceGrpc.NotifServiceImplBase {
    /** Horodatage de démarrage du service, pour le calcul de l'uptime. */
    private final long startTime = System.currentTimeMillis();

    /**
     * Reçoit un événement, le journalise et confirme sa prise en charge.
     *
     * @param req  requête contenant l'événement et sa charge utile
     * @param resp observateur de flux sur lequel est renvoyée la confirmation
     */
    @Override
    public void notify(NotifRequest req, StreamObserver<NotifResponse> resp) {
        System.out.println("[Notif] event=" + req.getEvent() + " payload=" + req.getPayload());
        resp.onNext(NotifResponse.newBuilder().setAccepted(true).build());
        resp.onCompleted();
    }

    /**
     * Retourne l'état de santé du service (statut, uptime, charge CPU).
     *
     * @param req  requête de HealthCheck
     * @param resp observateur de flux sur lequel est renvoyée la mesure de santé
     */
    @Override
    public void healthCheck(HealthReq req, StreamObserver<HealthResp> resp) {
        resp.onNext(HealthUtil.build(startTime));
        resp.onCompleted();
    }
}