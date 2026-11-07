package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.HealthResp;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public final class HealthUtil {
    private HealthUtil() {
    }

    public static HealthResp build(long startTime) {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpu = os.getCpuLoad();
        if (Double.isNaN(cpu) || cpu < 0) {
            cpu = 0.0;
        }
        String status = cpu > 0.90 ? "DEGRADED" : "SERVING"; // DOWN = service injoignable
        return HealthResp.newBuilder()
                .setStatus(status)
                .setUptimeMs(System.currentTimeMillis() - startTime)
                .setCpuLoad(cpu)
                .build();
    }
}