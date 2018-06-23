package com.deltanullnull.kenshoku;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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

    public static Recipe findRecipe(String name)
    {
        Recipe r = new Recipe(name);

        Document doc = Jsoup.parse("http://cookpad");



        return r;
    }

}
