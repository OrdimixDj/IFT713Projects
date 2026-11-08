package ca.usherbrooke.ift605.mapek;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Assemble la boucle MAPE-K complète (Monitor, Analyzer, Planner, Executor)
 * autour d'une {@link KnowledgeBase} partagée. Démarre le Monitor sur un thread
 * et exécute un cycle Analyze-Plan-Execute toutes les 10s.
 */
public class GestionnaireAutonome {
    // Différent du squelette : kb déclaré avant monitor/analyzer/executor.
    // Si kb est en dernier, il vaut null au moment de new Monitor(kb) -> erreur de
    // compilation.
    private final KnowledgeBase kb = new KnowledgeBase();
    private final Monitor monitor = new Monitor(kb);
    private final Analyzer analyzer = new Analyzer(kb);
    private final Planner planner = new Planner();
    private final Executor executor = new Executor(kb);

    /**
     * Démarre le Monitor sur un thread dédié, qui écoute au port IPC 9090 puis
     * planifie le cycle Analyze-Plan-Execute toutes les 10 secondes.
     */
    public void demarrer() {
        new Thread(() -> {
            try {
                monitor.demarrer(9090);
            } catch (IOException e) {
                System.err.println("[Gestionnaire] Monitor arrêté: " + e.getMessage());
            }
        }).start();

        ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
        sched.scheduleAtFixedRate(() -> {
            var symptomes = analyzer.analyser();
            if (!symptomes.isEmpty())
                System.out.println("[Analyze] " + symptomes.stream().map(s -> s.service + ":" + s.type).toList());
            var actions = planner.planifier(symptomes);
            if (!actions.isEmpty())
                System.out.println("[Plan] " + actions.stream().map(a -> a.service + ":" + a.type).toList());
            actions.forEach(executor::executer);
        }, 0, 10, TimeUnit.SECONDS); // cycle MAPE-K toutes les 10 s
    }

    /**
     * Point d'entrée : lance le gestionnaire autonome.
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {
        new GestionnaireAutonome().demarrer();
    }

    /**
     * @return la base de connaissances partagée, qui est utilisée par le
     *         ChaosMonkey
     */
    public KnowledgeBase getKb() {
        return kb;
    }
}