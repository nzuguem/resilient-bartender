package me.nzuguem.resilience.bartender;

public record Beer(String name) {

    public static Beer of(String name) {
        return new Beer(name);
    }
}
