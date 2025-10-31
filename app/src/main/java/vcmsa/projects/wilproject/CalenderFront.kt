package vcmsa.projects.wilproject


import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import vcmsa.projects.wilproject.db.EddieDatabase
import vcmsa.projects.wilproject.models.CalendarSchedule
import vcmsa.projects.wilproject.event.CalendarEvent
import vcmsa.projects.wilproject.viewModel.CalendarViewModel
import vcmsa.projects.wilproject.firebase.CalendarRepos
import vcmsa.projects.wilproject.firebase.FirebaseDB
import java.text.SimpleDateFormat
import java.util.*
import kotlin.getValue

class CalenderFront : AppCompatActivity() {

    private lateinit var filterSpinner: Spinner
    private lateinit var addEventButton: Button
    private lateinit var selectedDateText: TextView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var sessionManager: SessionManager
//(Islam, 2025)
    private val viewModel: CalendarViewModel by lazy {

        val database = EddieDatabase.getDatabase(applicationContext)
        val calendarDao = database.calenderDao()
        val firebaseConnect = FirebaseDB()

        val calendarRepository = CalendarRepos(calendarDao, firebaseConnect)

        val factory = CalendarViewModel.provideFactory(calendarRepository, sessionManager)

        ViewModelProvider(this, factory)[CalendarViewModel::class.java]
    }

    private lateinit var eventsAdapter: EventsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes_calender)

        sessionManager = SessionManager(applicationContext)

        filterSpinner = findViewById(R.id.filterSpinner)
        addEventButton = findViewById(R.id.addEventButton)
        selectedDateText = findViewById(R.id.calendarPageTitle)
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView)
        bottomNavigation = findViewById(R.id.bottom_navigation)


        setupBottomNavigation()
        setupRecyclerView()
        setupFilterSpinner()
        setupAddEventButton()

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                eventsAdapter.submitList(state.events)
                updateSelectedDateText(state.selectedDate)
            }
        }
    }

    private fun setupBottomNavigation() {

        bottomNavigation.selectedItemId = R.id.btnCalendar

        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnHome -> {
                    // Navigate to HomePage
                    val intent = Intent(this, HomePage::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.btnCalendar -> {

                    true
                }
                R.id.btnNotes -> {

                    val intent = Intent(this, NotesFront::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.btnTextbook -> {
                    // Navigate to Textbook or show coming soon
                    val intent = Intent(this, TextbookList::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.btnAI -> {
                    // Navigate to AI/Chat page
                    val intent = Intent(this, ChatHistory::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }


    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter { event ->
            viewModel.onEvent(CalendarEvent.deleteEvent(event))
        }

        eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CalenderFront)
            adapter = eventsAdapter
        }
    }
//(Islam, 2025)
    private fun setupFilterSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this@CalenderFront,
            R.array.event_types,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        filterSpinner.adapter = adapter
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()

                val filterType = if (selectedItem == "All Events") null else selectedItem
                viewModel.onEvent(CalendarEvent.filterByType(filterType))
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
//show event dialog (Islam, 2025)
    private fun showAddEventDialog() {
        val dialogView = LayoutInflater.from(this@CalenderFront).inflate(R.layout.event_dialog, null)
        val dialog = AlertDialog.Builder(this@CalenderFront)
            .setView(dialogView)
            .create()

        val eventNameEditText = dialogView.findViewById<EditText>(R.id.eventNameEditText)
        val eventDescriptionEditText = dialogView.findViewById<EditText>(R.id.eventDescriptionEditText)
        val eventTypeSpinner = dialogView.findViewById<Spinner>(R.id.eventTypeSpinner)
        val eventDatePicker = dialogView.findViewById<DatePicker>(R.id.eventDatePicker)
        val saveEventButton = dialogView.findViewById<Button>(R.id.saveEventButton)

        val typeAdapter = ArrayAdapter.createFromResource(
            this@CalenderFront,
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
//(Islam, 2025)
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