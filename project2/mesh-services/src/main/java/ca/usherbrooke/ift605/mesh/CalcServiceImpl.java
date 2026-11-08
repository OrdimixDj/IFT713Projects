package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.CalcRequest;
import ca.usherbrooke.ift605.mesh.proto.CalcResponse;
import ca.usherbrooke.ift605.mesh.proto.CalcServiceGrpc;
import ca.usherbrooke.ift605.mesh.proto.HealthReq;
import ca.usherbrooke.ift605.mesh.proto.HealthResp;
import io.grpc.stub.StreamObserver;

/**
 * Implémentation gRPC du service de calcul. Expose une opération métier
 * {@code compute} (addition, soustraction, multiplication, division) qui
 * retourne le résultat et sa latence, ainsi qu'un {@code healthCheck}, délégué
 * à {@link HealthUtil}.
 */
public class CalcServiceImpl extends CalcServiceGrpc.CalcServiceImplBase {
    /** Horodatage de démarrage du service, pour le calcul de l'uptime. */
    private final long startTime = System.currentTimeMillis();

    /**
     * Exécute l'opération arithmétique demandée et renvoie le résultat, ainsi que
     * la latence mesurée. Un opérateur inconnu produit un résultat {@code NaN}.
     * 
     * @param req  requête contenant les opérandes {@code a}, {@code b} et
     *             l'opérateur {@code op}
     * @param resp observateur de flux sur lequel est renvoyée la réponse
     */
    @Override
    public void compute(CalcRequest req, StreamObserver<CalcResponse> resp) {
        long t0 = System.currentTimeMillis();
        double r;

        switch (req.getOp()) {
            case "+":
                r = req.getA() + req.getB();
                break;
            case "-":
                r = req.getA() - req.getB();
                break;
            case "*":
                r = req.getA() * req.getB();
                break;
            case "/":
                r = req.getB() != 0 ? req.getA() / req.getB() : Double.NaN;
                break;
            default:
                r = Double.NaN;
                break;
        }

        resp.onNext(CalcResponse.newBuilder()
                .setResult(r)
                .setLatencyMs(System.currentTimeMillis() - t0)
                .build());
        resp.onCompleted();
    }

    /**
     * Retourne l'état de santé du service (statut, uptime, charge CPU).
     *
     * @param req  requête de HealthCheck (nom du service)
     * @param resp observateur de flux sur lequel est renvoyée la mesure de santé
     */
    @Override
    public void healthCheck(HealthReq req, StreamObserver<HealthResp> resp) {
        resp.onNext(HealthUtil.build(startTime));
        resp.onCompleted();
    }
}