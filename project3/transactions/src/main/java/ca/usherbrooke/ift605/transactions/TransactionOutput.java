package ca.usherbrooke.ift605.transactions;

import java.io.Serializable;

public record TransactionOutput(long amount, String scriptPubKey) implements Serializable {}