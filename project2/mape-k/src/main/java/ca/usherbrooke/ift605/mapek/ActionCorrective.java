package ca.usherbrooke.ift605.mapek;

public class ActionCorrective {
    public final String service;
    public final TypeAction type;

    public ActionCorrective(String service, TypeAction type) {
        this.service = service;
        this.type = type;
    }
}