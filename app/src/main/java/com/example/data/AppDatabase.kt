package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RawMaterial::class,
        Formulation::class,
        FormulationRevision::class,
        FormulationItem::class,
        ProductionLog::class,
        PriceHistoryEntry::class,
        ProductionOrder::class,
        ProductionOrderItem::class,
        ProductionOrderEvent::class,
        RecipePhase::class,
        RecipeItem::class,
        RecipeStatus::class,
        ProductionOrderPhase::class,
        ProductionOrderRecipeItem::class,
        QualityTest::class,
        FormulationQualityTest::class,
        ProductionOrderQualityTest::class,
        ProductionOrderTestRecord::class,
        ProductionAdjustment::class,
        DevelopmentProject::class,
        DevelopmentSample::class,
        SyncMetadata::class,
        LabSession::class,
        LabTest::class,
        LabAttachment::class,
        FormulationReferenceSpecs::class,
        OperationalAlert::class,
        RecycleBinItem::class
    ],
    version = 37, // Upgraded database version to include status fields in DevelopmentSample
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun gbrDao(): GbrDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gbr_paints_production_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON;")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
