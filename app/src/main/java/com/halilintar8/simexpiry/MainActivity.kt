package com.halilintar8.simexpiry

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.halilintar8.simexpiry.data.SimCard
import com.halilintar8.simexpiry.data.SimCardDatabase
import com.halilintar8.simexpiry.worker.AlarmScheduler
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    // --- UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SimCardAdapter
    private var isFabMenuOpen = false

    // --- Data ---
    private val simCardList = mutableListOf<SimCard>()
    private val simCardDao by lazy { SimCardDatabase.getDatabase(this).simCardDao() }

    // --- Preferences ---
    private lateinit var sharedPrefs: SharedPreferences
    private var alarmHour = DEFAULT_ALARM_HOUR
    private var alarmMinute = DEFAULT_ALARM_MINUTE
    private var reminderDays = DEFAULT_REMINDER_DAYS

    // --- Permissions ---
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Toast.makeText(
            this,
            if (granted) "Notifications enabled" else "Notifications disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "MainActivity started")

        // Init shared preferences
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSavedPreferences()

        // Setup system features
        createNotificationChannelIfNeeded()
        checkNotificationPermissionIfNeeded()

        // Setup UI
        setupRecyclerView()
        observeSimCards()
        setupFabMenu()

        // Schedule daily alarm
        AlarmScheduler.scheduleDailyAlarm(this, alarmHour, alarmMinute, reminderDays)
    }

    // --- Preferences ---
    private fun loadSavedPreferences() {
        alarmHour = sharedPrefs.getInt(KEY_ALARM_HOUR, DEFAULT_ALARM_HOUR)
        alarmMinute = sharedPrefs.getInt(KEY_ALARM_MINUTE, DEFAULT_ALARM_MINUTE)
        reminderDays = sharedPrefs.getInt(KEY_REMINDER_DAYS, DEFAULT_REMINDER_DAYS)
    }

    // --- RecyclerView setup ---
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        adapter = SimCardAdapter(
            simCards = simCardList,
            onEditClick = { pos -> showSimCardDialog(true, pos) },
            onDeleteClick = { pos -> deleteSimCard(pos) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun observeSimCards() {
        simCardDao.getAllSimCards().observe(this, Observer { simCards ->
            simCardList.clear()
            simCardList.addAll(simCards)
            adapter.notifyDataSetChanged()
            Log.d(TAG, "SIM list updated (${simCardList.size} items)")
        })
    }

    // --- Add / Edit Dialog ---
    private fun showSimCardDialog(isEdit: Boolean, position: Int = -1) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sim_card, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etSimNumber = dialogView.findViewById<EditText>(R.id.etSimNumber)
        val etExpiredDate = dialogView.findViewById<EditText>(R.id.etExpiredDate)

        if (isEdit && position >= 0) {
            val simCard = simCardList[position]
            etName.setText(simCard.name)
            etSimNumber.setText(simCard.simCardNumber)
            etExpiredDate.setText(simCard.expiredDate)
        }

        // Date picker
        etExpiredDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d -> etExpiredDate.setText("%04d-%02d-%02d".format(y, m + 1, d)) },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Dialog
        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit SIM Card" else "Add SIM Card")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                val name = etName.text.toString().trim()
                val simNumber = etSimNumber.text.toString().trim()
                val expiredDate = etExpiredDate.text.toString().trim()

                if (name.isBlank() || simNumber.isBlank() || expiredDate.isBlank()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    if (isEdit && position >= 0) {
                        simCardDao.update(
                            simCardList[position].copy(
                                name = name,
                                simCardNumber = simNumber,
                                expiredDate = expiredDate
                            )
                        )
                    } else {
                        simCardDao.insert(
                            SimCard(
                                id = 0,
                                name = name,
                                simCardNumber = simNumber,
                                expiredDate = expiredDate
                            )
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSimCard(position: Int) {
        lifecycleScope.launch { simCardDao.delete(simCardList[position]) }
    }

    // --- Notifications ---
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SIM Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifications for SIM card expiry alerts" }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // --- Time & Reminder Setup ---
    private fun openTimePickerDialog() {
        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                alarmHour = selectedHour
                alarmMinute = selectedMinute
                sharedPrefs.edit()
                    .putInt(KEY_ALARM_HOUR, alarmHour)
                    .putInt(KEY_ALARM_MINUTE, alarmMinute)
                    .apply()

                Toast.makeText(
                    this,
                    "Notification time set to %02d:%02d".format(alarmHour, alarmMinute),
                    Toast.LENGTH_SHORT
                ).show()

                AlarmScheduler.scheduleDailyAlarm(this, alarmHour, alarmMinute, reminderDays)
            },
            alarmHour, alarmMinute, true
        ).show()
    }

    private fun showReminderDaysDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(reminderDays.toString())
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Set Reminder Days")
            .setMessage("Enter how many days before expiry you want to be notified:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val entered = input.text.toString().toIntOrNull()
                if (entered != null && entered in 1..365) {
                    reminderDays = entered
                    sharedPrefs.edit().putInt(KEY_REMINDER_DAYS, reminderDays).apply()

                    Toast.makeText(
                        this,
                        "Reminder set to $reminderDays days before expiry",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Refresh alarms + UI
                    AlarmScheduler.scheduleDailyAlarm(this, alarmHour, alarmMinute, reminderDays)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Enter a valid number (1–365)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- FAB Menu ---
    private fun setupFabMenu() {
        val fabMain = findViewById<FloatingActionButton>(R.id.fabMain)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        val fabSetAlarm = findViewById<FloatingActionButton>(R.id.fabSetAlarm)
        val fabSetReminder = findViewById<FloatingActionButton>(R.id.fabSetReminder)

        fabMain.setOnClickListener { toggleFabMenu(fabAdd, fabSetAlarm, fabSetReminder) }
        fabAdd.setOnClickListener {
            showSimCardDialog(false)
            toggleFabMenu(fabAdd, fabSetAlarm, fabSetReminder)
        }
        fabSetAlarm.setOnClickListener {
            openTimePickerDialog()
            toggleFabMenu(fabAdd, fabSetAlarm, fabSetReminder)
        }
        fabSetReminder.setOnClickListener {
            showReminderDaysDialog()
            toggleFabMenu(fabAdd, fabSetAlarm, fabSetReminder)
        }
    }

    private fun toggleFabMenu(vararg fabs: FloatingActionButton) {
        isFabMenuOpen = !isFabMenuOpen
        fabs.forEach { fab ->
            fab.visibility = if (isFabMenuOpen) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val PREFS_NAME = "sim_prefs"
        private const val KEY_ALARM_HOUR = "alarm_hour"
        private const val KEY_ALARM_MINUTE = "alarm_minute"
        private const val KEY_REMINDER_DAYS = "reminder_days"

        private const val DEFAULT_ALARM_HOUR = 7
        private const val DEFAULT_ALARM_MINUTE = 0
        private const val DEFAULT_REMINDER_DAYS = 7

        private const val CHANNEL_ID = "sim_expiry_channel"
    }
}
