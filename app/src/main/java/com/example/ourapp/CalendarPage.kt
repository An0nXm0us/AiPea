package com.example.ourapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatImageButton // Import AppCompatImage
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity that shows a calendar and list of events.
 * - User can select date or toggle weekly period.
 * - User can filter by type via spinner.
 * - User can add events via a dialog.
 */
class CalendarPage : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var filterSpinner: Spinner
    private lateinit var addEventButton: Button
    private lateinit var selectedDateText: TextView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var backButton: ImageButton
    private lateinit var switchButton: SwitchCompat

    private lateinit var viewModel: CalendarViewModel
    private lateinit var eventsAdapter: EventsAdapter

    // Database instance
    private lateinit var database: CalendarDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_page)

        // Initialize database and viewmodel
        database = CalendarDatabase.getDatabase(applicationContext)
        val factory = CalendarViewModelFactory(database.dao)
        viewModel = ViewModelProvider(this, factory)[CalendarViewModel::class.java]

        // find views
        calendarView = findViewById(R.id.calendarView)
        filterSpinner = findViewById(R.id.filterSpinner)
        addEventButton = findViewById(R.id.saveBtn)
        selectedDateText = findViewById(R.id.calendarPageTitle)
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView)
        backButton = findViewById(R.id.backBtn)
        switchButton = findViewById(R.id.displayPeriod)

        setupRecyclerView()
        setupCalendar()
        setupFilterSpinner()
        setupAddEventButton()
        setupSwitch()

        // Observe view model state and bind to UI
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                eventsAdapter.submitList(state.events)
                updateSelectedDateText(state.selectedDate)
            }
        }

        backButton.setOnClickListener { finish() }
    }

    /**
     * Configure RecyclerView and adapter
     */
    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter { event ->
            // Delete click callback
            viewModel.onEvent(CalendarEvent.deleteEvent(event))
        }

        eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CalendarPage)
            adapter = eventsAdapter
        }
    }

    /**
     * Hook calendar date selection to the ViewModel.
     */
    private fun setupCalendar() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            viewModel.onEvent(CalendarEvent.selectDate(cal.time))
        }
    }

    /**
     * Configure filter spinner to send filter events to ViewModel.
     */
    private fun setupFilterSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this@CalendarPage,
            R.array.event_types,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        filterSpinner.adapter = adapter
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedType = if (position == 0) null else parent.getItemAtPosition(position).toString()
                viewModel.onEvent(CalendarEvent.filterByType(selectedType))
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                viewModel.onEvent(CalendarEvent.filterByType(null))
            }
        }
    }

    /**
     * Show dialog to add an event, read input and forward to ViewModel.
     */
    private fun setupAddEventButton() {
        addEventButton.setOnClickListener { showAddEventDialog() }
    }

    private fun setupSwitch() {
        // Toggle weekly period mode
        switchButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onEvent(CalendarEvent.togglePeriod(isChecked))
        }
    }

    private fun showAddEventDialog() {
        val dialogView = LayoutInflater.from(this@CalendarPage).inflate(R.layout.event_dialog, null)
        val dialog = AlertDialog.Builder(this@CalendarPage)
            .setView(dialogView)
            .create()

        val eventNameEditText = dialogView.findViewById<EditText>(R.id.eventNameEditText)
        val eventDescriptionEditText = dialogView.findViewById<EditText>(R.id.eventDescriptionEditText)
        val eventTypeSpinner = dialogView.findViewById<Spinner>(R.id.eventTypeSpinner)
        val eventDatePicker = dialogView.findViewById<DatePicker>(R.id.eventDatePicker)
        val saveEventButton = dialogView.findViewById<Button>(R.id.saveEventButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelEventButton)

        // spinner adapter uses same resource
        val typeAdapter = ArrayAdapter.createFromResource(
            this@CalendarPage,
            R.array.event_types,
            android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        eventTypeSpinner.adapter = typeAdapter

        saveEventButton.setOnClickListener {
            val cal = Calendar.getInstance().apply {
                set(eventDatePicker.year, eventDatePicker.month, eventDatePicker.dayOfMonth)
            }

            viewModel.onEvent(CalendarEvent.setEventName(eventNameEditText.text.toString()))
            viewModel.onEvent(CalendarEvent.setEventDescription(eventDescriptionEditText.text.toString()))
            viewModel.onEvent(CalendarEvent.setEventType(eventTypeSpinner.selectedItem.toString()))
            viewModel.onEvent(CalendarEvent.setEventDate(cal.time))
            viewModel.onEvent(CalendarEvent.saveEvent)

            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Update top title to show currently selected date (for clarity).
     */
    private fun updateSelectedDateText(date: Date) {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        selectedDateText.text = "Events for ${dateFormat.format(date)}"
    }

    /**
     * EventsAdapter: inner RecyclerView.Adapter implementation.
     * - Binds CalendarSchedule to event_item.xml layout.
     * - onDeleteClick is invoked when the delete button is tapped.
     */
    private inner class EventsAdapter(
        private val onDeleteClick: (CalendarSchedule) -> Unit
    ) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

        private var events = emptyList<CalendarSchedule>()

        /**
         * Replace adapter list and refresh UI.
         */
        fun submitList(newEvents: List<CalendarSchedule>) {
            events = newEvents
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.event_item, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            holder.bind(events[position])
        }

        override fun getItemCount(): Int = events.size

        inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTv: TextView = itemView.findViewById(R.id.eventNameTextView)
            private val descTv: TextView = itemView.findViewById(R.id.eventDescriptionTextView)
            private val dateTv: TextView = itemView.findViewById(R.id.eventDateTextView)
            private val typeTv: TextView = itemView.findViewById(R.id.eventTypeTextView)
            private val deleteBtn: Button = itemView.findViewById(R.id.deleteEventButton)

            /**
             * Bind event fields to views and set delete listener.
             */
            fun bind(event: CalendarSchedule) {
                nameTv.text = event.eventName
                descTv.text = event.eventDescription
                typeTv.text = event.eventType

                val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                dateTv.text = dateFormat.format(event.eventDate)

                deleteBtn.setOnClickListener {
                    onDeleteClick(event)
                }
            }
        }
    }
}
