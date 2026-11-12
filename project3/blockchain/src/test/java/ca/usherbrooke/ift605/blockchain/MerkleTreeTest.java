package ca.usherbrooke.ift605.blockchain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MerkleTreeTest {

    // Nombre impair de feuilles (5) : la dernière est dupliquée et toutes les preuves restent valides
    @Test
    void preuveValide_pourChaqueTransaction_nombreImpair() {
        List<String> txs = List.of("tx0", "tx1", "tx2", "tx3", "tx4");
        MerkleTree<String> tree = new MerkleTree<>(txs);
        for (int i = 0; i < txs.size(); i++) {
            List<byte[]> proof = tree.getMerkleProof(i);
            assertTrue(MerkleTree.verifyProof(MerkleTree.leafHash(txs.get(i)), proof, tree.getMerkleRoot()));
        }
    }

    // Une transaction qui n'est pas dans l'arbre est rejetée
    @Test
    void preuveRejetee_pourTransactionAbsente() {
        MerkleTree<String> tree = new MerkleTree<>(List.of("tx0", "tx1", "tx2", "tx3"));
        List<byte[]> proof = tree.getMerkleProof(1);
        assertFalse(MerkleTree.verifyProof(MerkleTree.leafHash("txPirate"), proof, tree.getMerkleRoot()));
    }

    // 1024 transactions donc preuve de 10 hachages
    @Test
    void tailleDePreuve_logarithmique() {
        List<String> txs = new ArrayList<>();
        for (int i = 0; i < 1024; i++) {
            txs.add("tx" + i);
        }
        assertEquals(10, new MerkleTree<>(txs).getMerkleProof(500).size());
    }
}