package com.nanosoft22.mcquizadmin;

public class CategoryModel {

    private String name;
    private int sets;
    String key;

    public CategoryModel(){
        //for firebase
    }

    public CategoryModel(String name, int sets,String key) {
        this.name = name;
        this.sets = sets;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
