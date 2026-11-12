package ca.usherbrooke.ift605.transactions;

import ca.usherbrooke.ift605.crypto.CryptoUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Stack;

public final class P2PKHScript {

    private static final byte[] TRUE = {1};
    private static final byte[] FALSE = {0};

    private P2PKHScript() {}

    public static boolean execute(byte[] signature, byte[] publicKey, String address, byte[] signedData) {
        if (signature == null || publicKey == null) {
            return false; // entrée non signée
        }
        Stack<byte[]> stack = new Stack<>();

        stack.push(signature);
        stack.push(publicKey);

        opDup(stack);
        opHash160(stack);
        stack.push(address.getBytes(StandardCharsets.UTF_8));
        if (!opEqualVerify(stack)) {
            return false;
        }
        opCheckSig(stack, signedData);

        return Arrays.equals(stack.pop(), TRUE);
    }

    static void opDup(Stack<byte[]> stack) {
        stack.push(stack.peek());
    }

    static void opHash160(Stack<byte[]> stack) {
        byte[] publicKey = stack.pop();
        stack.push(CryptoUtils.addressFromBytes(publicKey).getBytes(StandardCharsets.UTF_8));
    }

    static boolean opEqualVerify(Stack<byte[]> stack) {
        return Arrays.equals(stack.pop(), stack.pop());
    }

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