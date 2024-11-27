package com.example.badminton.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.badminton.data.booking.Booking
import com.example.badminton.data.booking.BookingCourt
import com.example.badminton.data.booking.BookingDao
import com.example.badminton.data.booking.BookingCourtDao
import com.example.badminton.data.court.Court
import com.example.badminton.data.court.CourtDao
import com.example.badminton.data.review.Review
import com.example.badminton.data.review.ReviewDao
import com.example.badminton.data.user.User
import com.example.badminton.data.user.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

@Database(
    entities = [User::class, Court::class, Booking::class, BookingCourt::class, Review::class],
    version = 10, // Increment the database version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courtDao(): CourtDao
    abstract fun bookingDao(): BookingDao
    abstract fun bookingCourtDao(): BookingCourtDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("AppDatabase", "Database created, populating with dummy data")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = getDatabase(context)
                    populateDatabase(database)
                    Log.d("AppDatabase", "Dummy data population complete")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error populating database", e)
                }
            }
        }
    }
}

suspend fun populateDatabase(db: AppDatabase) {
    val courtDao = db.courtDao()

    Log.d("AppDatabase", "Inserting dummy courts")
    val courts = listOf(
        Court(courtName = "Court A", courtType = "Rubber", courtPrice = 50.00),
        Court(courtName = "Court B", courtType = "Rubber", courtPrice = 50.00),
        Court(courtName = "Court C", courtType = "Rubber", courtPrice = 50.00),
        Court(courtName = "Court D", courtType = "Rubber", courtPrice = 50.00),
        Court(courtName = "Court E", courtType = "Wooden", courtPrice = 30.00),
        Court(courtName = "Court F", courtType = "Wooden", courtPrice = 30.00),
        Court(courtName = "Court G", courtType = "Wooden", courtPrice = 30.00),
        Court(courtName = "Court H", courtType = "Wooden", courtPrice = 30.00)
    )
    courtDao.insertAll(courts)
    Log.d("AppDatabase", "Dummy courts inserted")
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
