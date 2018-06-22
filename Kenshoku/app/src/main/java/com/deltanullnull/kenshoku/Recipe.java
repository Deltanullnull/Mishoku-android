package com.deltanullnull.kenshoku;

import java.util.ArrayList;
import java.util.Arrays;

public class Recipe
{
    private String name;
    //private Ingredient[] ingredients;
    private ArrayList<Ingredient> ingredients;

    public Recipe(String name, Ingredient [] ingredients)
    {
        this.name = name;
        this.ingredients = new ArrayList<Ingredient>(Arrays.asList(ingredients));
    }

    public Recipe(String name)
    {
        this.name = name;
    }

    public void addIngredient(Ingredient ingredient)
    {
        ingredients.add(ingredient);
    }

}
