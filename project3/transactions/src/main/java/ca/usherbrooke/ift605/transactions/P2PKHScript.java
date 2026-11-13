package ca.usherbrooke.ift605.transactions;

import ca.usherbrooke.ift605.crypto.CryptoUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Stack;

/**
 * Exécution simplifiée d'un script Pay-to-PubKeyHash sur une pile.
 * Phase 1 (scriptSig) : {@code <signature> <pubKey>}.
 * Phase 2 (scriptPubKey) :
 * {@code OP_DUP OP_HASH160 <adresse> OP_EQUALVERIFY OP_CHECKSIG}.
 */
public final class P2PKHScript {

    // Valeurs empilées par OP_CHECKSIG
    private static final byte[] TRUE = { 1 };
    private static final byte[] FALSE = { 0 };

    private P2PKHScript() {
    }

    /**
     * Exécute les deux phases du script P2PKH.
     *
     * @param signature  la signature du scriptSig
     * @param publicKey  la clé publique encodée du scriptSig
     * @param address    l'adresse du scriptPubKey (destinataire de l'UTXO)
     * @param signedData les données signées (l'id de la transaction)
     * @return {@code true} si la clé correspond à l'adresse et que la signature est
     *         valide
     */
    public static boolean execute(byte[] signature, byte[] publicKey, String address, byte[] signedData) {
        if (signature == null || publicKey == null) {
            return false; // entrée non signée
        }
        Stack<byte[]> stack = new Stack<>();

        // Phase 1 — scriptSig : <signature> <pubKey>
        stack.push(signature);
        stack.push(publicKey);
        // pile : [signature, pubKey]

        // Phase 2 — scriptPubKey : OP_DUP OP_HASH160 <adresse> OP_EQUALVERIFY
        // OP_CHECKSIG
        opDup(stack); // [signature, pubKey, pubKey]
        opHash160(stack); // [signature, pubKey, adresse(pubKey)]
        stack.push(address.getBytes(StandardCharsets.UTF_8)); // [signature, pubKey, adresse(pubKey), adresse]
        if (!opEqualVerify(stack)) { // [signature, pubKey]
            return false; // la clé publique fournie n'est pas celle du destinataire de l'UTXO
        }
        opCheckSig(stack, signedData); // [TRUE] ou [FALSE]

        // Le script réussit si le sommet de la pile est TRUE
        return Arrays.equals(stack.pop(), TRUE);
    }

    /** OP_DUP : duplique le sommet de la pile (la clé publique). */
    static void opDup(Stack<byte[]> stack) {
        stack.push(stack.peek());
    }

    /**
     * OP_HASH160 (simplifié) : remplace la clé publique par son adresse
     * {@code Base58(SHA-256(pk))}.
     */
    static void opHash160(Stack<byte[]> stack) {
        byte[] publicKey = stack.pop();
        stack.push(CryptoUtils.addressFromBytes(publicKey).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * OP_EQUALVERIFY : retire les deux éléments du sommet et vérifie qu'ils sont
     * égaux.
     *
     * @return {@code true} si l'adresse calculée est égale à l'adresse attendue
     */
    static boolean opEqualVerify(Stack<byte[]> stack) {
        return Arrays.equals(stack.pop(), stack.pop());
    }

    /**
     * OP_CHECKSIG : retire la clé publique et la signature, et empile TRUE si la
     * signature est valide, FALSE sinon.
     *
     * @param signedData les données signées (l'id de la transaction)
     */
    static void opCheckSig(Stack<byte[]> stack, byte[] signedData) {
        byte[] publicKey = stack.pop();
        byte[] signature = stack.pop();
        boolean ok;
        try {
            ok = CryptoUtils.verify(signedData, signature, CryptoUtils.decodePublicKey(publicKey));
        } catch (IllegalArgumentException e) {
            ok = false; // cas où la clé publique est mal formée
        }
        stack.push(ok ? TRUE : FALSE);
    }
}