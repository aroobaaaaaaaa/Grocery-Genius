package com.rendox.grocerygenius.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.rendox.grocerygenius.database.grocery.GroceryDao


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): AppDatabase = Room.databaseBuilder(
        context,

        AppDatabase::class.java,
        "grocery-genius-database"
    ).build()
    @Provides
    fun provideGroceryDao(database: AppDatabase): GroceryDao {
        return database.groceryDao()
    }

}