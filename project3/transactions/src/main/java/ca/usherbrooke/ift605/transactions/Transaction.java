package ca.usherbrooke.ift605.transactions;

import ca.usherbrooke.ift605.crypto.CryptoUtils;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Transaction {

    public static final long SATOSHIS_PER_BTC = 100_000_000L;

    public static final String COINBASE_TX_ID = "0".repeat(64);

    private final List<TransactionInput> inputs;
    private final List<TransactionOutput> outputs;

    public Transaction(List<TransactionInput> inputs, List<TransactionOutput> outputs) {
        this.inputs = new ArrayList<>(inputs);
        this.outputs = new ArrayList<>(outputs);
    }

    // height rend chaque coinbase unique, sinon deux récompenses identiques auraient le même id
    public static Transaction coinbase(String toAddress, long amount, int height) {
        return new Transaction(
                List.of(new TransactionInput(COINBASE_TX_ID, height)),
                List.of(new TransactionOutput(amount, toAddress)));
    }

    public boolean isCoinbase() {
        return inputs.size() == 1 && COINBASE_TX_ID.equals(inputs.get(0).getTxId());
    }

    public String getId() {
        StringBuilder sb = new StringBuilder();
        for (TransactionInput in : inputs) {
            sb.append(in.getTxId()).append(':').append(in.getOutputIndex()).append(';');
        }
        sb.append('|');
        for (TransactionOutput out : outputs) {
            sb.append(out.amount()).append(':').append(out.scriptPubKey()).append(';');
        }
        return CryptoUtils.sha256Hex(sb.toString());
    }

    public void sign(PrivateKey sk, PublicKey pk) {
        byte[] signature = CryptoUtils.sign(getId().getBytes(StandardCharsets.UTF_8), sk);
        for (TransactionInput in : inputs) {
            in.setScriptSig(signature, pk.getEncoded());
        }
    }

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

    public long getTotalOutput() {
        long total = 0;
        for (TransactionOutput out : outputs) {
            total += out.amount();
        }
        return total;
    }

    public List<TransactionInput> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    public List<TransactionOutput> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    @Override
    public String toString() {
        return getId();
    }
}