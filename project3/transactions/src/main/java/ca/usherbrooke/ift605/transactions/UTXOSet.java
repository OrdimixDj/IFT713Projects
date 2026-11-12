package ca.usherbrooke.ift605.transactions;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UTXOSet {

    private final Map<OutPoint, TransactionOutput> utxos = new HashMap<>();

    public void addTransaction(Transaction tx) {
        if (!tx.isCoinbase()) {
            for (TransactionInput in : tx.getInputs()) {
                utxos.remove(new OutPoint(in.getTxId(), in.getOutputIndex()));
            }
        }
        String txId = tx.getId();
        List<TransactionOutput> outputs = tx.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            utxos.put(new OutPoint(txId, i), outputs.get(i));
        }
    }

    public TransactionOutput get(String txId, int index) {
        return utxos.get(new OutPoint(txId, index));
    }

    public boolean isValid(Transaction tx) {
        if (tx.isCoinbase()) {
            return false; // une coinbase ne dépense rien : elle n'est pas validée par ces règles
        }
        byte[] signedData = tx.getId().getBytes(StandardCharsets.UTF_8);
        Set<OutPoint> alreadyUsed = new HashSet<>();
        long totalIn = 0;

        for (TransactionInput in : tx.getInputs()) {
            OutPoint outPoint = new OutPoint(in.getTxId(), in.getOutputIndex());

            // Même UTXO utilisée deux fois dans la même transaction
            if (!alreadyUsed.add(outPoint)) {
                return false;
            }

            TransactionOutput utxo = utxos.get(outPoint);
            if (utxo == null) {
                return false;
            }

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

        return totalIn >= tx.getTotalOutput();
    }
}