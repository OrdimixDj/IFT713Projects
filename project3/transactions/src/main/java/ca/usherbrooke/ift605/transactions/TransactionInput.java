package ca.usherbrooke.ift605.transactions;

import java.io.Serializable;

/**
 * Entrée de transaction : référence à l'UTXO dépensée et scriptSig
 * (signature + clé publique) qui prouve le droit de la dépenser.
 */
public class TransactionInput implements Serializable {

    private final String txId;
    private final int outputIndex;

    // scriptSig est rempli au moment de la signature
    private byte[] signature;
    private byte[] publicKey;

    /**
     * Crée une entrée non signée.
     *
     * @param txId        l'id de la transaction qui contient l'UTXO dépensée
     * @param outputIndex l'index de cette sortie dans la transaction
     */
    public TransactionInput(String txId, int outputIndex) {
        this.txId = txId;
        this.outputIndex = outputIndex;
    }

    /**
     * Remplit le scriptSig ({@code <signature> <pubKey>}).
     *
     * @param signature la signature ECDSA de la transaction
     * @param publicKey la clé publique encodée du signataire
     */
    public void setScriptSig(byte[] signature, byte[] publicKey) {
        this.signature = signature;
        this.publicKey = publicKey;
    }

    /** @return l'id de la transaction référencée */
    public String getTxId() {
        return txId;
    }

    /** @return l'index de la sortie référencée */
    public int getOutputIndex() {
        return outputIndex;
    }

    /**
     * @return la signature du scriptSig, ou {@code null} si l'entrée n'est pas
     *         signée
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * @return la clé publique encodée du scriptSig, ou {@code null} si l'entrée
     *         n'est pas signée
     */
    public byte[] getPublicKey() {
        return publicKey;
    }
}