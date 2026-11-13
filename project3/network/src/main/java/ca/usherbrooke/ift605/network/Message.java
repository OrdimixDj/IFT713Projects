package ca.usherbrooke.ift605.network;

import java.io.Serializable;

public record Message(MessageType type, int senderPort, Serializable payload) implements Serializable {
}