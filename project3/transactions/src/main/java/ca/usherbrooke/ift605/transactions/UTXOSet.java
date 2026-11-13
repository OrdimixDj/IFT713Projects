package ca.usherbrooke.ift605.transactions;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ensemble des sorties non dépensées (UTXO). Permet de valider les transactions
 * et de calculer les soldes.
 */
public class UTXOSet {

    // Clé = (txId, index) de la sortie ; valeur = montant + adresse
    private final Map<OutPoint, TransactionOutput> utxos = new HashMap<>();

    /**
     * Applique une transaction : retire les UTXO qu'elle dépense et ajoute ses
     * nouvelles sorties.
     *
     * @param tx la transaction déjà validée
     */
    public void addTransaction(Transaction tx) {
        // Les entrées dépensées sortent de l'ensemble
        if (!tx.isCoinbase()) {
            for (TransactionInput in : tx.getInputs()) {
                utxos.remove(new OutPoint(in.getTxId(), in.getOutputIndex()));
            }
        }
        // Chaque sortie devient une nouvelle UTXO, identifiée par (id de cette
        // transaction, position)
        String txId = tx.getId();
        List<TransactionOutput> outputs = tx.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            utxos.put(new OutPoint(txId, i), outputs.get(i));
        }
    }

    /**
     * @param txId  l'id de la transaction
     * @param index l'index de la sortie
     * @return l'UTXO correspondante, ou {@code null} si elle n'existe pas ou est
     *         déjà dépensée
     */
    public TransactionOutput get(String txId, int index) {
        return utxos.get(new OutPoint(txId, index));
    }

    /**
     * Retourne toutes les UTXO verrouillées sur une adresse, et est utilisé par le
     * portefeuille.
     *
     * @param address l'adresse
     * @return les UTXO de cette adresse, indexées par leur {@link OutPoint}
     */
    public Map<OutPoint, TransactionOutput> findByAddress(String address) {
        Map<OutPoint, TransactionOutput> result = new HashMap<>();
        for (Map.Entry<OutPoint, TransactionOutput> e : utxos.entrySet()) {
            if (e.getValue().scriptPubKey().equals(address)) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * Vérifie une transaction :
     * <ol>
     * <li>toutes les entrées référencent des UTXO existantes (et chacune une seule
     * fois) ;</li>
     * <li>somme(entrées) ≥ somme(sorties), et chaque sortie a un montant positif
     * ;</li>
     * <li>chaque signature est valide.</li>
     * </ol>
     *
     * @param tx la transaction à vérifier
     * @return {@code true} si elle est valide ; toujours {@code false} pour une
     *         coinbase
     */
    public boolean isValid(Transaction tx) {
        if (tx.isCoinbase()) {
            return false; // une coinbase ne dépense rien : elle n'est pas validée par ces règles
        }
        // Chaque scriptSig a signé l'id de la transaction
        byte[] signedData = tx.getId().getBytes(StandardCharsets.UTF_8);
        Set<OutPoint> alreadyUsed = new HashSet<>();
        long totalIn = 0;

        for (TransactionInput in : tx.getInputs()) {
            OutPoint outPoint = new OutPoint(in.getTxId(), in.getOutputIndex());

            // Même UTXO utilisée deux fois dans la même transaction
            if (!alreadyUsed.add(outPoint)) {
                return false;
            }

            // l'entrée référence une UTXO existante (sinon : inexistante ou déjà dépensée)
            TransactionOutput utxo = utxos.get(outPoint);
            if (utxo == null) {
                return false;
            }

            // la signature est valide pour l'adresse de l'UTXO
            if (!P2PKHScript.execute(in.getSignature(), in.getPublicKey(), utxo.scriptPubKey(), signedData)) {
                return false;
            }
            totalIn += utxo.amount();
        }

        for (TransactionOutput out : tx.getOutputs()) {
            if (out.amount() <= 0) {
                return false; // pas de montant nul ou négatif
            }
        }

        // somme(entrées) ≥ somme(sorties) ; la différence correspond aux frais
        return totalIn >= tx.getTotalOutput();
    }
}