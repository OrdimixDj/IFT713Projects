package ca.usherbrooke.ift605.transactions;

import ca.usherbrooke.ift605.crypto.CryptoUtils;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Transaction Bitcoin simplifiée : une liste d'entrées et une liste de sorties.
 * L'id est {@code SHA-256(inputs + outputs)}, calculé sans les signatures
 * (comme SegWit) : signer ne change pas l'id.
 */
public class Transaction implements Serializable {

    /** Nombre de satoshis dans 1 BTC. */
    public static final long SATOSHIS_PER_BTC = 100_000_000L;

    /** Id fictif référencé par l'entrée d'une transaction coinbase (64 zéros). */
    public static final String COINBASE_TX_ID = "0".repeat(64);

    private final List<TransactionInput> inputs;
    private final List<TransactionOutput> outputs;

    /**
     * Crée une transaction non signée.
     *
     * @param inputs  les UTXO dépensées
     * @param outputs les nouvelles sorties
     */
    public Transaction(List<TransactionInput> inputs, List<TransactionOutput> outputs) {
        this.inputs = new ArrayList<>(inputs);
        this.outputs = new ArrayList<>(outputs);
    }

    /**
     * Crée une transaction coinbase : la récompense du mineur, sans vraie entrée.
     * Son entrée spéciale contient la hauteur du bloc pour que chaque coinbase ait
     * un id unique.
     *
     * @param toAddress l'adresse du mineur
     * @param amount    la récompense, en satoshis
     * @param height    la hauteur du bloc
     * @return la transaction coinbase
     */
    public static Transaction coinbase(String toAddress, long amount, int height) {
        return new Transaction(
                List.of(new TransactionInput(COINBASE_TX_ID, height)),
                List.of(new TransactionOutput(amount, toAddress)));
    }

    /**
     * @return {@code true} si c'est une transaction coinbase
     */
    public boolean isCoinbase() {
        return inputs.size() == 1 && COINBASE_TX_ID.equals(inputs.get(0).getTxId());
    }

    /**
     * Calcule l'id : SHA-256 des entrées et des sorties, sans les signatures.
     *
     * @return l'id en hexadécimal
     */
    public String getId() {
        StringBuilder sb = new StringBuilder();
        // Les signatures (scriptSig) sont exclues : on signe l'id, il ne peut donc pas
        // dépendre de la signature. Les séparateurs ':' ';' '|' évitent que deux
        // transactions différentes donnent la même chaîne
        for (TransactionInput in : inputs) {
            sb.append(in.getTxId()).append(':').append(in.getOutputIndex()).append(';');
        }
        sb.append('|');
        for (TransactionOutput out : outputs) {
            sb.append(out.amount()).append(':').append(out.scriptPubKey()).append(';');
        }
        return CryptoUtils.sha256Hex(sb.toString());
    }

    /**
     * Signe l'id de la transaction et place le scriptSig dans chaque entrée.
     *
     * @param sk la clé privée du propriétaire des UTXO
     * @param pk la clé publique correspondante
     */
    public void sign(PrivateKey sk, PublicKey pk) {
        byte[] signature = CryptoUtils.sign(getId().getBytes(StandardCharsets.UTF_8), sk);
        // Simplification : une seule signature, copiée dans toutes les entrées
        for (TransactionInput in : inputs) {
            in.setScriptSig(signature, pk.getEncoded());
        }
    }

    /**
     * Calcule les frais de transaction : somme des entrées − somme des sorties.
     *
     * @param utxoSet l'ensemble UTXO qui contient les sorties dépensées
     * @return les frais, en satoshis
     * @throws IllegalArgumentException si une entrée référence une UTXO inconnue
     */
    public long getFee(UTXOSet utxoSet) {
        long in = 0;
        for (TransactionInput input : inputs) {
            TransactionOutput utxo = utxoSet.get(input.getTxId(), input.getOutputIndex());
            if (utxo == null) {
                throw new IllegalArgumentException("UTXO inconnue : " + input.getTxId() + ":" + input.getOutputIndex());
            }
            in += utxo.amount();
        }
        return in - getTotalOutput();
    }

    /**
     * @return la somme des montants des sorties, en satoshis
     */
    public long getTotalOutput() {
        long total = 0;
        for (TransactionOutput out : outputs) {
            total += out.amount();
        }
        return total;
    }

    /** @return les entrées (liste non modifiable) */
    public List<TransactionInput> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    /** @return les sorties (liste non modifiable) */
    public List<TransactionOutput> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    /**
     * @return l'id de la transaction, utilisé pour la feuille de Merkle
     */
    @Override
    public String toString() {
        return getId();
    }
}