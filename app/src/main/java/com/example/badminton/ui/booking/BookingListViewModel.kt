package com.example.badminton.ui.booking

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.badminton.data.booking.Booking
import com.example.badminton.data.booking.BookingCourtRepository
import com.example.badminton.data.booking.BookingRepository
import com.example.badminton.data.court.Court
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class BookingListViewModel(
    private val bookingRepository: BookingRepository,
    private val bookingCourtRepository: BookingCourtRepository,
    context: Context
) : ViewModel() {

    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _bookingsWithCourts = MutableStateFlow<List<Pair<Booking, List<Court>>>>(emptyList())
    val bookingsWithCourts: StateFlow<List<Pair<Booking, List<Court>>>> get() = _bookingsWithCourts

    private val _pastBookingsWithCourts = MutableStateFlow<List<Pair<Booking, List<Court>>>>(emptyList())
    val pastBookingsWithCourts: StateFlow<List<Pair<Booking, List<Court>>>> get() = _pastBookingsWithCourts

    private fun fetchUserId(): Int? {
        return sharedPreferences.getString("user_id", null)?.toIntOrNull()
    }

    val userId = fetchUserId()

    init {
        fetchUpcomingBookingsWithCourts()
        fetchPastBookingsWithCourts()
    }

    private fun fetchUpcomingBookingsWithCourts() {
        viewModelScope.launch {
            userId?.let { userId ->
                bookingRepository.getBookingsByUserId(userId).collect { bookingsList ->
                    val currentDateTime = LocalDateTime.now()
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                    val bookingsWithCourts = bookingsList.filter { booking ->
                        try {
                            val bookingDate = booking.bookingDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            val bookingTime = LocalTime.parse(booking.startTime, timeFormatter)
                            val combinedDateTime = bookingDate.withHour(bookingTime.hour).withMinute(bookingTime.minute)
                            combinedDateTime.isAfter(currentDateTime)
                        } catch (e: Exception) {
                            Log.e("BookingListViewModel", "Failed to parse date: ${booking.bookingDate} ${booking.startTime}", e)
                            false
                        }
                    }.map { booking ->
                        val courts = bookingRepository.getCourtsForBooking(booking.bookingId)
                        booking to courts
                    }.sortedBy { it.first.bookingDate }
                    _bookingsWithCourts.value = bookingsWithCourts
                }
            }
        }
    }


    private fun fetchPastBookingsWithCourts() {
        viewModelScope.launch {
            userId?.let {
                val currentTime = Calendar.getInstance().time
                val dateTimeFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy HH:mm", Locale.getDefault())
                val pastBookingsWithCourts = mutableListOf<Pair<Booking, List<Court>>>()

                bookingRepository.getBookingsByUserId(it).collect { pastBookingsList ->
                    pastBookingsList.forEach { booking ->
                        val dateTimeString = "${booking.bookingDate} ${booking.startTime}"
                        try {
                            val bookingDateTime = dateTimeFormat.parse(dateTimeString)
                            if (bookingDateTime != null && bookingDateTime.before(currentTime)) {
                                val courts = bookingRepository.getCourtsForBooking(booking.bookingId)
                                pastBookingsWithCourts.add(booking to courts)
                            }
                        } catch (e: ParseException) {
                            Log.e("BookingListViewModel", "Failed to parse date: $dateTimeString", e)
                        }
                    }
                    pastBookingsWithCourts.sortBy { bookingCourtPair ->
                        val dateTimeString = "${bookingCourtPair.first.bookingDate} ${bookingCourtPair.first.startTime}"
                        try {
                            dateTimeFormat.parse(dateTimeString)
                        } catch (e: ParseException) {
                            Log.e("BookingListViewModel", "Failed to parse date for sorting: $dateTimeString", e)
                            null
                        }
                    }
                    _pastBookingsWithCourts.value = pastBookingsWithCourts
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun cancelBooking(booking: Booking) {
        viewModelScope.launch {
            try {
                Log.d("BookingListViewModel", "Cancelling booking: ${booking.bookingId}")
                bookingRepository.deleteBookingAndRelatedCourts(booking.bookingId)
                fetchUpcomingBookingsWithCourts()
                fetchPastBookingsWithCourts()
                Log.d("BookingListViewModel", "Booking cancelled: ${booking.bookingId}")
            } catch (e: Exception) {
                Log.e("BookingListViewModel", "Failed to cancel booking", e)
            }
        }
    }
}
