package ca.usherbrooke.ift605.demo;

import ca.usherbrooke.ift605.blockchain.Block;
import ca.usherbrooke.ift605.blockchain.Blockchain;
import ca.usherbrooke.ift605.blockchain.MerkleTree;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Démonstration du module 2, via une preuve d'appartenance dans un arbre de
 * Merkle et la propriété tamper-evident de la chaîne.
 */
public final class BlockchainDemo {

    private BlockchainDemo() {
    }

    /**
     * Affiche les deux démonstrations dans la console.
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {
        merkleProof();
        tamperEvident();
    }

    // Preuve qu'une transaction appartient à un bloc, sans connaître les autres
    // transactions
    private static void merkleProof() {
        System.out.println("=== Arbre de Merkle : 5 transactions (nombre impair donc la dernière est dupliquée) ===");
        List<String> txs = List.of("Alice → Bob : 10 BTC", "Bob → Charlie : 5 BTC", "Charlie → Dave : 2 BTC",
                "Dave → Eve : 1 BTC", "Eve → Alice : 3 BTC");
        MerkleTree<String> tree = new MerkleTree<>(txs);
        byte[] root = tree.getMerkleRoot();
        System.out.println("Racine de Merkle : " + HexFormat.of().formatHex(root).substring(0, 16) + "…");

        List<byte[]> proof = tree.getMerkleProof(2);
        System.out.println("Preuve pour « Charlie → Dave : 2 BTC » : " + proof.size() + " hachages");
        System.out.println("  Transaction authentique             → valide : "
                + MerkleTree.verifyProof(MerkleTree.leafHash("Charlie → Dave : 2 BTC"), proof, root));
        System.out.println("  Transaction falsifiée (200 BTC)     → valide : "
                + MerkleTree.verifyProof(MerkleTree.leafHash("Charlie → Dave : 200 BTC"), proof, root));

        List<String> many = new ArrayList<>();
        for (int i = 0; i < 1024; i++)
            many.add("tx" + i);
        System.out.println("Pour un bloc de 1024 transactions, la preuve ne fait que "
                + new MerkleTree<>(many).getMerkleProof(500).size() + " hachages");
    }

    // Une chaîne de 3 blocs, puis une modification frauduleuse dans le bloc #1
    private static void tamperEvident() {
        System.out.println();
        System.out.println("=== Propriété tamper-evident : chaîne de 3 blocs ===");
        DemoTx fraud = new DemoTx("Bob → Charlie : 5 BTC");
        Blockchain<DemoTx> chain = new Blockchain<>();
        chain.addBlock(new Block<>(Blockchain.GENESIS_PREVIOUS_HASH, List.of(new DemoTx("Alice → Bob : 10 BTC"))));
        chain.addBlock(new Block<>(chain.getLatestBlock().computeHash(), List.of(fraud)));
        chain.addBlock(
                new Block<>(chain.getLatestBlock().computeHash(), List.of(new DemoTx("Charlie → Dave : 2 BTC"))));
        for (int h = 0; h < chain.size(); h++) {
            Block<DemoTx> b = chain.getBlock(h);
            System.out.println("Bloc #" + h + " : " + b.getTransactions().get(0)
                    + " | previousHash = " + b.getHeader().previousHash().substring(0, 12) + "…");
        }
        System.out.println("Chaîne valide : " + chain.isValid());

        System.out.println("Modification frauduleuse du bloc #1 : 5 BTC deviennent 500 BTC");
        fraud.data = "Bob → Charlie : 500 BTC";
        System.out.println("Chaîne valide : " + chain.isValid() + " → modification détectée");
    }

    // Transaction modifiable, utilisée seulement pour simuler la fraude (les vraies
    // transactions sont normalement immuables)
    private static final class DemoTx {
        private String data;

        DemoTx(String data) {
            this.data = data;
        }

        // Contenu haché par MerkleTree.leafHash
        @Override
        public String toString() {
            return data;
        }
    }
}