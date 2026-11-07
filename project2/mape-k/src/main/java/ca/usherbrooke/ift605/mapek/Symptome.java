package ca.usherbrooke.ift605.mapek;

public class Symptome {
    public final String service;
    public final TypeSymptome type;

    public Symptome(String service, TypeSymptome type) {
        this.service = service;
        this.type = type;
    }
}