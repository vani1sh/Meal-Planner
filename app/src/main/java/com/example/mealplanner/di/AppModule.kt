package com.example.mealplanner.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mealplanner.data.local.AppDatabase
import com.example.mealplanner.data.local.FoodDao
import com.example.mealplanner.data.remote.OpenFoodFactsApi
import com.example.mealplanner.data.repository.FoodRepositoryImpl
import com.example.mealplanner.domain.repository.FoodRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(): OpenFoodFactsApi {
        return Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `recipes` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `calories` REAL NOT NULL,
                        `protein` REAL NOT NULL,
                        `fat` REAL NOT NULL,
                        `carbs` REAL NOT NULL,
                        PRIMARY KEY(`id`))"""
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recipes ADD COLUMN ingredientsJson TEXT NOT NULL DEFAULT '[]'")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shopping_list` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `amountGrams` INTEGER NOT NULL, 
                    `note` TEXT
                )"""
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "meal_planner_db"
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            //.fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideFoodDao(db: AppDatabase): FoodDao {
        return db.foodDao()
    }

    @Provides
    @Singleton
    fun provideFoodRepository(api: OpenFoodFactsApi, dao: FoodDao): FoodRepository {
        return FoodRepositoryImpl(api, dao)
    }
}