package com.justsoft.nulpschedule.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.justsoft.nulpschedule.db.dao.*
import com.justsoft.nulpschedule.db.model.EntityScheduleClass
import com.justsoft.nulpschedule.db.model.EntitySchedule
import com.justsoft.nulpschedule.db.model.EntitySubject
import com.justsoft.nulpschedule.db.typeconverters.DayOfWeekTypeConverter
import com.justsoft.nulpschedule.db.typeconverters.LocalDateTimeToLongTypeConverter
import com.justsoft.nulpschedule.db.typeconverters.LocalDateToLongTypeConverter
import com.justsoft.nulpschedule.db.typeconverters.ScheduleTypeConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Database(
    version = 13,
    exportSchema = true,
    entities = [
        EntitySubject::class,
        EntitySchedule::class,
        EntityScheduleClass::class
    ]
)
@TypeConverters(
    DayOfWeekTypeConverter::class,
    LocalDateToLongTypeConverter::class,
    LocalDateTimeToLongTypeConverter::class,
    ScheduleTypeConverter::class,
)
abstract class ScheduleDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao
    abstract fun classesDao(): ScheduleClassDao
    abstract fun subjectDao(): SubjectDao


    companion object {

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Subject ADD COLUMN customName TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE `Group`")
                database.execSQL("DROP TABLE Institute")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Schedule ADD COLUMN subgroup INTEGER DEFAULT 1 NOT NULL")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Subject ADD COLUMN scheduleId INTEGER DEFAULT null NOT NULL")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Schedule ADD COLUMN addTime INTEGER NOT NULL")
                database.execSQL("UPDATE Schedule SET addTime = updateTime")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ScheduleClass ADD COLUMN teacherName TEXT DEFAULT '' NOT NULL")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Schedule ADD COLUMN position INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Schedule ADD COLUMN scheduleType INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("UPDATE Schedule SET scheduleType = 0")
            }
        }

        // For Singleton instantiation
        @Volatile
        private var instance: ScheduleDatabase? = null

        fun getInstance(context: Context): ScheduleDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): ScheduleDatabase {
            return Room
                .databaseBuilder(context, ScheduleDatabase::class.java, "shedule-db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(database: SupportSQLiteDatabase) {
                        super.onCreate(database)
                        Log.d("ScheduleDatabase", "Creating database")
                    }
                })
                .fallbackToDestructiveMigrationFrom(2)
                .fallbackToDestructiveMigrationFrom(10)
                .addMigrations(
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_11_12,
                    MIGRATION_12_13
                )
                .build()
        }
    }
}

@Module
@InstallIn(ApplicationComponent::class)
object ScheduleDatabaseModule {

    @Provides
    @Singleton
    fun provideSubjectDao(@ApplicationContext context: Context): SubjectDao {
        return ScheduleDatabase.getInstance(context).subjectDao()
    }

    @Provides
    @Singleton
    fun provideScheduleClassesDao(@ApplicationContext context: Context): ScheduleClassDao {
        return ScheduleDatabase.getInstance(context).classesDao()
    }

    @Provides
    @Singleton
    fun provideScheduleDao(@ApplicationContext context: Context): ScheduleDao {
        return ScheduleDatabase.getInstance(context).scheduleDao()
    }
}