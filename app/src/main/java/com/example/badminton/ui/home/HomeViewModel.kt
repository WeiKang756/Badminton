package com.example.badminton.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.badminton.data.booking.Booking
import com.example.badminton.data.booking.BookingRepository
import com.example.badminton.data.court.Court
import com.example.badminton.data.court.CourtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeViewModel(
    private val courtRepository: CourtRepository,
    private val bookingRepository: BookingRepository,
    context: Context
) : ViewModel() {

    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _courts = MutableStateFlow<List<Court>>(emptyList())
    val courts: StateFlow<List<Court>> get() = _courts

    private val _upcomingBookings = MutableStateFlow<List<Booking>>(emptyList())
    val upcomingBookings: StateFlow<List<Booking>> get() = _upcomingBookings

    private val _currentDateBookings = MutableStateFlow<List<Pair<Booking, Court>>>(emptyList())
    val currentDateBookings: StateFlow<List<Pair<Booking, Court>>> get() = _currentDateBookings

    private fun fetchUserId(): Int? {
        val userId = sharedPreferences.getString("user_id", null)?.toIntOrNull()
        if (userId != null) {
            Log.d("HomeViewModel", "Successfully accessed SharedPreferences: User ID = $userId")
        } else {
            Log.d("HomeViewModel", "Failed to access SharedPreferences: User ID not found")
        }
        return userId
    }

    val userId = fetchUserId()

    fun fetchUpcomingBookings() {
        viewModelScope.launch {
            val currentTime = Calendar.getInstance().time
            val upcomingBookings = mutableListOf<Booking>()

            if (userId != null) {
                val userBookings = bookingRepository.getBookingsByUserId(userId).first()
                userBookings.forEach { booking ->
                    val bookingDate = booking.bookingDate // Ensure this is in the correct format
                    val startTime = booking.startTime // Ensure this is in the correct format

                    val dateTimeString = "$bookingDate $startTime"
                    val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy HH:mm", Locale.getDefault())

                    try {
                        val bookingDateTime = dateFormat.parse(dateTimeString)
                        if (bookingDateTime != null && bookingDateTime.after(currentTime)) {
                            upcomingBookings.add(booking)
                        }
                    } catch (e: ParseException) {
                        Log.e("HomeViewModel", "Failed to parse date: $dateTimeString", e)
                    }
                }

                // Sort the bookings by date
                upcomingBookings.sortBy { booking ->
                    val dateTimeString = "${booking.bookingDate} ${booking.startTime}"
                    val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy HH:mm", Locale.getDefault())
                    try {
                        dateFormat.parse(dateTimeString)
                    } catch (e: ParseException) {
                        Log.e("HomeViewModel", "Failed to parse date for sorting: $dateTimeString", e)
                        null
                    }
                }
            }

            // Now you have a sorted list of upcoming bookings for the user
            _upcomingBookings.value = upcomingBookings
        }
    }

    fun fetchCurrentDateBookings() {
        viewModelScope.launch {
            val currentTime = Calendar.getInstance().time
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentTime)
            val currentBookings = mutableListOf<Pair<Booking, Court>>()

            if (userId != null) {
                val userBookings = bookingRepository.getBookingsByUserId(userId).first()
                userBookings.forEach { booking ->
                    val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(booking.bookingDate)
                    if (bookingDate == currentDate) {
                        val courts = bookingRepository.getCourtsForBooking(booking.bookingId)
                        courts.forEach { court ->
                            currentBookings.add(booking to court)
                        }
                    }
                }
            }
            _currentDateBookings.value = currentBookings
        }
    }
}
