package com.finansio.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.finansio.app.data.local.dao.BudgetDao
import com.finansio.app.data.local.dao.CategoryDao
import com.finansio.app.data.local.dao.TransactionDao
import com.finansio.app.data.local.entity.BudgetEntity
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.local.entity.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, BudgetEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1 -> v2: menambah tabel anggaran. Data transaksi dan kategori yang ada tetap utuh. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budgets` (" +
                        "`categoryId` INTEGER NOT NULL, " +
                        "`monthlyLimit` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`categoryId`))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finansio_database"
                )
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    prepopulateDatabase(database.categoryDao())
                }
            }
        }

        fun prepopulateDatabase(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Gaji", type = TransactionType.INCOME, colorHex = "#C6F24E"),
                CategoryEntity(name = "Freelance", type = TransactionType.INCOME, colorHex = "#B7B4F0"),
                CategoryEntity(name = "Investasi", type = TransactionType.INCOME, colorHex = "#F2735C"),
                CategoryEntity(name = "Lainnya", type = TransactionType.INCOME, colorHex = "#F0D95B"),
                CategoryEntity(name = "Makanan", type = TransactionType.EXPENSE, colorHex = "#C6F24E"),
                CategoryEntity(name = "Transportasi", type = TransactionType.EXPENSE, colorHex = "#B7B4F0"),
                CategoryEntity(name = "Belanja", type = TransactionType.EXPENSE, colorHex = "#F2735C"),
                CategoryEntity(name = "Hiburan", type = TransactionType.EXPENSE, colorHex = "#F0D95B")
            )
            defaultCategories.forEach { categoryDao.insert(it) }
        }
    }
}
