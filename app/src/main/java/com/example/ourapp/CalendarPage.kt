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

class CalendarPage : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var filterSpinner: Spinner
    private lateinit var addEventButton: Button
    private lateinit var selectedDateText: TextView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var switchButton: SwitchCompat

    private lateinit var viewModel: CalendarViewModel
    private lateinit var eventsAdapter: EventsAdapter

    // Database instance
    private lateinit var database: CalendarDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_page)

        // Initialize database
        database = CalendarDatabase.getDatabase(applicationContext)

        // Initialize ViewModel with factory
        val viewModelFactory = CalendarViewModelFactory(database.dao)
        viewModel = ViewModelProvider(this, viewModelFactory)[CalendarViewModel::class.java]

        // Initialize views using findViewById
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

        // Observe events changes
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                eventsAdapter.submitList(state.events)
                updateSelectedDateText(state.selectedDate)
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter { event ->
            viewModel.onEvent(CalendarEvent.deleteEvent(event))
        }

        eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CalendarPage)
            adapter = eventsAdapter
        }
    }

    private fun setupCalendar() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            viewModel.onEvent(CalendarEvent.selectDate(calendar.time))
        }
    }

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

    private fun setupAddEventButton() {
        addEventButton.setOnClickListener {
            showAddEventDialog()
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

        val typeAdapter = ArrayAdapter.createFromResource(
            this@CalendarPage,
            R.array.event_types,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        eventTypeSpinner.adapter = typeAdapter

        saveEventButton.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                set(eventDatePicker.year, eventDatePicker.month, eventDatePicker.dayOfMonth)
            }

            viewModel.onEvent(CalendarEvent.setEventName(eventNameEditText.text.toString()))
            viewModel.onEvent(CalendarEvent.setEventDescription(eventDescriptionEditText.text.toString()))
            viewModel.onEvent(CalendarEvent.setEventType(eventTypeSpinner.selectedItem.toString()))
            viewModel.onEvent(CalendarEvent.setEventDate(calendar.time))
            viewModel.onEvent(CalendarEvent.saveEvent)

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateSelectedDateText(date: Date) {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        selectedDateText.text = "Events for ${dateFormat.format(date)}"
    }

    private inner class EventsAdapter(
        private val onDeleteClick: (CalendarSchedule) -> Unit
    ) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

        private var events = emptyList<CalendarSchedule>()

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
            fun bind(event: CalendarSchedule) {
                itemView.findViewById<TextView>(R.id.eventNameTextView).text = event.eventName
                itemView.findViewById<TextView>(R.id.eventDescriptionTextView).text = event.eventDescription
                val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                itemView.findViewById<TextView>(R.id.eventDateTextView).text = dateFormat.format(event.eventDate)
                itemView.findViewById<TextView>(R.id.eventTypeTextView).text = event.eventType
                itemView.findViewById<Button>(R.id.deleteEventButton).setOnClickListener {
                    onDeleteClick(event)
                }
            }
        }
    }
}