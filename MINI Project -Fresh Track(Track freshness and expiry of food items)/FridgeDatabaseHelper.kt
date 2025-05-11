package com.example.fridgemanager1

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FridgeDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "FridgeDB", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE FoodItems(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, quantity TEXT, expiry TEXT)"
            )
            db.execSQL(
                "CREATE TABLE Recipes(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, ingredients TEXT, steps TEXT)"
            )
        }


        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS FoodItems")
        onCreate(db)
    }

    fun insertItem(item: FridgeItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", item.name)
            put("quantity", item.quantity)
            put("expiry", item.expiryDate)
        }

        return db.insert("FoodItems", null, values)
    }


    fun getAllItems(): MutableList<FridgeItem> {
        val itemList = mutableListOf<FridgeItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM FoodItems", null)
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val quantity = cursor.getString(cursor.getColumnIndexOrThrow("quantity"))
            val expiry = cursor.getString(cursor.getColumnIndexOrThrow("expiry"))
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            itemList.add(FridgeItem(id, name, quantity, expiry))

        }
        cursor.close()
        return itemList
    }

    fun updateItem(id: Int, name: String, quantity: String, expiry: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("name", name)
            put("quantity", quantity)
            put("expiry", expiry)
        }
        return db.update("FoodItems", contentValues, "id=?", arrayOf(id.toString())) > 0
    }

    fun deleteItem(id: Int): Boolean {
        val db = this.writableDatabase
        return db.delete("FoodItems", "id=?", arrayOf(id.toString())) > 0
    }
    fun getAllRecipes(): List<Recipe> {
        val recipeList = mutableListOf<Recipe>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Recipes", null)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val ingredients = cursor.getString(cursor.getColumnIndexOrThrow("ingredients"))
            val steps = cursor.getString(cursor.getColumnIndexOrThrow("steps"))

            recipeList.add(Recipe(id, title, ingredients, steps))
        }
        cursor.close()
        return recipeList
    }
    fun getSuggestedRecipes(fridgeItems: List<FridgeItem>): List<Recipe> {
        val allRecipes = getAllRecipes()
        val suggestions = mutableListOf<Recipe>()

        val fridgeIngredients = fridgeItems.map { it.name.lowercase().trim() }

        for (recipe in allRecipes) {
            val recipeIngredients = recipe.ingredients.lowercase().split(",").map { it.trim() }
            if (recipeIngredients.any { it in fridgeIngredients }) {
                suggestions.add(recipe)
            }
        }

        return suggestions
    }


}
