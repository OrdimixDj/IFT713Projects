package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.*;
import io.grpc.stub.StreamObserver;

public class CalcServiceImpl extends CalcServiceGrpc.CalcServiceImplBase {
    private final long startTime = System.currentTimeMillis();

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
                return;
        }

        resp.onNext(CalcResponse.newBuilder()
                .setResult(r)
                .setLatencyMs(System.currentTimeMillis() - t0)
                .build());
        resp.onCompleted();
    }

    @Override
    public void healthCheck(HealthReq req, StreamObserver<HealthResp> resp) {
        resp.onNext(HealthUtil.build(startTime));
        resp.onCompleted();
    }
}