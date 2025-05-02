package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: Category)

    @Query("SELECT * FROM categories Where userId = :userId")
    suspend fun getAllForUser(userId: Int): List<Category>

    // get all categories for a user, output in a list of strings
    @Query("SELECT name FROM categories WHERE userId = :userId")
    suspend fun getAllNamesForUser(userId: Int): List<String>

    @Query("SELECT name FROM categories WHERE userId = :userId")
    suspend fun getNameForId(userId: Int): String


}