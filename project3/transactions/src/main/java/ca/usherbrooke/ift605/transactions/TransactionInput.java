package ca.usherbrooke.ift605.transactions;

public class TransactionInput {

    private final String txId;
    private final int outputIndex;

    // scriptSig est rempli au moment de la signature
    private byte[] signature;
    private byte[] publicKey;

    public TransactionInput(String txId, int outputIndex) {
        this.txId = txId;
        this.outputIndex = outputIndex;
    }

    public void setScriptSig(byte[] signature, byte[] publicKey) {
        this.signature = signature;
        this.publicKey = publicKey;
    }

    public String getTxId() {
        return txId;
    }

    public int getOutputIndex() {
        return outputIndex;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }
}