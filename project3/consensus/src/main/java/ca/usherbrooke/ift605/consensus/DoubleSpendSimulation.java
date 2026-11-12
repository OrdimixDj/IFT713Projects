package ca.usherbrooke.ift605.consensus;

import ca.usherbrooke.ift605.blockchain.Block;
import ca.usherbrooke.ift605.blockchain.Blockchain;
import ca.usherbrooke.ift605.crypto.CryptoUtils;
import ca.usherbrooke.ift605.transactions.Transaction;
import ca.usherbrooke.ift605.transactions.TransactionInput;
import ca.usherbrooke.ift605.transactions.TransactionOutput;
import ca.usherbrooke.ift605.transactions.UTXOSet;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ca.usherbrooke.ift605.transactions.Transaction.SATOSHIS_PER_BTC;

public final class DoubleSpendSimulation {

    static final int DIFFICULTY = 3;
    static final int CONFIRMATIONS = 6;
    static final long BLOCK_REWARD = 50 * SATOSHIS_PER_BTC;

    private DoubleSpendSimulation() {}

    public static void main(String[] args) {
        run(0.30, new Random(42));

        System.out.println();
        System.out.println("=== Taux de réussite de l'attaque (10 000 courses simulées) ===");
        for (double share : new double[]{0.10, 0.30, 0.45, 0.55, 0.70}) {
            System.out.printf("Attaquant à %2.0f %% : attaque réussie dans %5.1f %% des cas%n",
                    share * 100, 100 * attackSuccessRate(share, 10_000, new Random(1)));
        }
    }

    // Retourne true si la chaîne honnête l'emporte
    public static boolean run(double attackerShare, Random random) {
        int honestPercent = (int) Math.round((1 - attackerShare) * 100);
        int attackerPercent = (int) Math.round(attackerShare * 100);

        KeyPair alice = CryptoUtils.generateKeyPair();
        String aliceAddress = CryptoUtils.publicKeyToAddress(alice.getPublic());
        String bobAddress = CryptoUtils.publicKeyToAddress(CryptoUtils.generateKeyPair().getPublic());
        String charlieAddress = CryptoUtils.publicKeyToAddress(CryptoUtils.generateKeyPair().getPublic());
        String honestMinerAddress = CryptoUtils.publicKeyToAddress(CryptoUtils.generateKeyPair().getPublic());
        String attackerAddress = CryptoUtils.publicKeyToAddress(CryptoUtils.generateKeyPair().getPublic());

        System.out.println("=== Chaîne commune : blocs #0 à #4 ===");
        Blockchain<Transaction> common = new Blockchain<>();
        Transaction aliceReward = Transaction.coinbase(aliceAddress, BLOCK_REWARD, 0);
        common.addBlock(Miner.mine(new Block<>(Blockchain.GENESIS_PREVIOUS_HASH, List.of(aliceReward)), DIFFICULTY));
        for (int height = 1; height <= 4; height++) {
            Transaction reward = Transaction.coinbase(honestMinerAddress, BLOCK_REWARD, height);
            common.addBlock(Miner.mine(new Block<>(common.getLatestBlock().computeHash(), List.of(reward)), DIFFICULTY));
        }
        UTXOSet commonUtxos = replay(common);

        // Alice envoie les mêmes 10 BTC à Bob ET à Charlie (même UTXO en entrée)
        Transaction toBob = pay(alice, aliceReward.getId(), aliceAddress, bobAddress, 10 * SATOSHIS_PER_BTC);
        Transaction toCharlie = pay(alice, aliceReward.getId(), aliceAddress, charlieAddress, 10 * SATOSHIS_PER_BTC);
        System.out.println();
        System.out.println("=== Double dépense : Alice → Bob (10 BTC) ET Alice → Charlie (10 BTC) ===");
        System.out.println("Chacune seule est valide : Bob=" + commonUtxos.isValid(toBob)
                + ", Charlie=" + commonUtxos.isValid(toCharlie));

        List<Block<Transaction>> honestBranch = new ArrayList<>();
        List<Block<Transaction>> attackerBranch = new ArrayList<>();
        while (honestBranch.size() < CONFIRMATIONS && attackerBranch.size() < CONFIRMATIONS) {
            boolean attackerFinds = random.nextDouble() < attackerShare;
            List<Block<Transaction>> branch = attackerFinds ? attackerBranch : honestBranch;
            int height = common.size() + branch.size();
            String previousHash = branch.isEmpty()
                    ? common.getLatestBlock().computeHash()
                    : branch.get(branch.size() - 1).computeHash();

            List<Transaction> txs = new ArrayList<>();
            txs.add(Transaction.coinbase(attackerFinds ? attackerAddress : honestMinerAddress, BLOCK_REWARD, height));
            if (branch.isEmpty()) {
                txs.add(attackerFinds ? toCharlie : toBob);
            }

            if (attackerFinds) {
                System.out.printf("Attaquant (%d%%) : %s #%d%s%n", attackerPercent,
                        branch.isEmpty() ? "tente de miner le bloc alternatif" : "mine le bloc", height,
                        branch.isEmpty() ? " avec tx BTC→Charlie" : "");
            } else {
                System.out.printf("Mineur honnête (%d%%) : mine bloc #%d%s%n", honestPercent, height,
                        branch.isEmpty() ? " avec tx BTC→Bob" : "");
            }
            branch.add(Miner.mine(new Block<>(previousHash, txs), DIFFICULTY));
        }

        Blockchain<Transaction> honestChain = extend(common, honestBranch);
        Blockchain<Transaction> attackerChain = extend(common, attackerBranch);
        boolean honestWins = honestChain.size() > attackerChain.size();
        Blockchain<Transaction> winner = honestWins ? honestChain : attackerChain;
        List<Block<Transaction>> losingBranch = honestWins ? attackerBranch : honestBranch;

        System.out.println();
        if (honestWins) {
            System.out.println("→ Après " + CONFIRMATIONS + " confirmations, la chaîne honnête est acceptée");
            System.out.println("→ L'attaquant a dépensé de l'énergie de calcul sans récompense ("
                    + losingBranch.size() + " bloc(s) abandonné(s))");
        } else {
            System.out.println("→ La chaîne de l'attaquant est plus longue : la double dépense a réussi");
        }

        UTXOSet finalUtxos = replay(winner);
        Transaction rejected = honestWins ? toCharlie : toBob;
        System.out.println("Chaîne retenue valide : " + winner.isValid()
                + " | transaction concurrente encore valide : " + finalUtxos.isValid(rejected)
                + " → double dépense détectée — transaction rejetée");
        return honestWins;
    }

    public static double attackSuccessRate(double attackerShare, int trials, Random random) {
        int successes = 0;
        for (int t = 0; t < trials; t++) {
            int honest = 0;
            int attacker = 0;
            while (honest < CONFIRMATIONS && attacker < CONFIRMATIONS) {
                if (random.nextDouble() < attackerShare) attacker++;
                else honest++;
            }
            if (attacker >= CONFIRMATIONS) successes++;
        }
        return (double) successes / trials;
    }

    // Alice paie amount à `to` et se renvoie le reste (sans frais)
    private static Transaction pay(KeyPair from, String utxoTxId, String fromAddress, String to, long amount) {
        Transaction tx = new Transaction(
                List.of(new TransactionInput(utxoTxId, 0)),
                List.of(new TransactionOutput(amount, to),
                        new TransactionOutput(BLOCK_REWARD - amount, fromAddress)));
        tx.sign(from.getPrivate(), from.getPublic());
        return tx;
    }

    private static Blockchain<Transaction> extend(Blockchain<Transaction> common, List<Block<Transaction>> branch) {
        Blockchain<Transaction> chain = new Blockchain<>();
        for (int i = 0; i < common.size(); i++) chain.addBlock(common.getBlock(i));
        for (Block<Transaction> b : branch) chain.addBlock(b);
        return chain;
    }

    private static UTXOSet replay(Blockchain<Transaction> chain) {
        UTXOSet utxos = new UTXOSet();
        for (int i = 0; i < chain.size(); i++) {
            for (Transaction tx : chain.getBlock(i).getTransactions()) {
                utxos.addTransaction(tx);
            }
        }
        return utxos;
    }
}