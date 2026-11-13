package ca.usherbrooke.ift605.blockchain;

// Transaction minimale : elle est modifiable exprès, pour simuler
// une fraude avec tamper() comme dans le test attendu de l'énoncé
class SimpleTx {

    private String data;

    SimpleTx(String data) {
        this.data = data;
    }

    // Modification frauduleuse : change le contenu, donc le hachage de la feuille
    // de Merkle
    void tamper() {
        data = data + " (modifiée)";
    }

    // Contenu haché par MerkleTree.leafHash
    @Override
    public String toString() {
        return data;
    }
}