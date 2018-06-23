package com.deltanullnull.kenshoku;

public class Ingredient {
    private String name;
    private int amount;
    private Unit unit;

    public Ingredient(String name, int amount, Unit unit)
    {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
    }

}
