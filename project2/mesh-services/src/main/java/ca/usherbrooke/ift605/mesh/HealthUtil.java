package ca.usherbrooke.ift605.mesh;

import ca.usherbrooke.ift605.mesh.proto.HealthResp;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

/**
 * Utilitaire partagé par les trois services pour construire leur réponse de
 * HealthCheck. Calcule le statut ({@code SERVING} ou {@code DEGRADED} selon la
 * charge CPU), l'uptime et la charge CPU courante. Le statut {@code DOWN} n'est
 * jamais produit ici : il est déduit par la {@link Sonde} lorsque le service
 * est injoignable.
 */
public final class HealthUtil {
    /** Classe utilitaire : ne fait rien mais est obligatoire. */
    private HealthUtil() {
    }

    /**
     * Construit une réponse de HealthCheck qui donne l'état courant du service.
     * 
     * @param startTime horodatage de démarrage du service, en epoch ms, utilisé
     *                  pour calculer l'uptime
     * @return la réponse contenant le statut, l'uptime et la charge CPU
     */
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