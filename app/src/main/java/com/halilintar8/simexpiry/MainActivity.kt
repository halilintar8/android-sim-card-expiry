package com.halilintar8.simexpiry

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.halilintar8.simexpiry.data.SimCard
import com.halilintar8.simexpiry.data.SimCardDatabase
import com.halilintar8.simexpiry.util.ReminderManager
import com.halilintar8.simexpiry.util.ThemeHelper
import com.halilintar8.simexpiry.worker.AlarmScheduler
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SimCardAdapter
    private val simCardList = mutableListOf<SimCard>()
    private val simCardDao by lazy { SimCardDatabase.getDatabase(this).simCardDao() }

    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabSetAlarm: FloatingActionButton
    private lateinit var fabSetReminder: FloatingActionButton
    private var isFabMenuOpen = false

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Toast.makeText(
                this,
                if (granted) "Notifications enabled" else "Notifications disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme
        ThemeHelper.applySavedThemeMode(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        observeSimCards()
        setupFabMenu()
        setupThemeToggle()
        checkNotificationPermissionIfNeeded()

        // Schedule alarm with saved time
        val (hour, minute) = ReminderManager.getAlarmTime(this)
        AlarmScheduler.scheduleDailyAlarm(this, hour, minute)
    }

    // --- RecyclerView ---
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        adapter = SimCardAdapter(
            simCardList,
            onEditClick = { pos -> showSimCardDialog(true, pos) },
            onDeleteClick = { pos -> deleteSimCard(pos) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun observeSimCards() {
        simCardDao.getAllSimCards().observe(this) { simCards ->
            simCardList.clear()
            simCardList.addAll(simCards)
            adapter.notifyDataSetChanged()
        }
    }

    // --- SIM Card Dialog ---
    private fun showSimCardDialog(isEdit: Boolean, position: Int = -1) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sim_card, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etSimNumber = dialogView.findViewById<EditText>(R.id.etSimNumber)
        val etExpiredDate = dialogView.findViewById<EditText>(R.id.etExpiredDate)

        // Sync expired date text color with name field
        etExpiredDate.setTextColor(etName.currentTextColor)
        etExpiredDate.setHintTextColor(etName.currentHintTextColor)

        if (isEdit && position >= 0) {
            val simCard = simCardList[position]
            etName.setText(simCard.name)
            etSimNumber.setText(simCard.simCardNumber)
            etExpiredDate.setText(simCard.expiredDate)
        }

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

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit SIM Card" else "Add SIM Card")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                val name = etName.text.toString().trim()
                val simNumber = etSimNumber.text.toString().trim()
                val expiredDate = etExpiredDate.text.toString().trim()
                if (name.isBlank() || simNumber.isBlank() || expiredDate.isBlank()) return@setPositiveButton

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
                        simCardDao.insert(SimCard(0, name, simNumber, expiredDate))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSimCard(position: Int) {
        lifecycleScope.launch { simCardDao.delete(simCardList[position]) }
    }

    // --- Alarm / Reminder ---
    private fun openTimePickerDialog() {
        val (hour, minute) = ReminderManager.getAlarmTime(this)
        TimePickerDialog(this, { _, h, m ->
            ReminderManager.setAlarmTime(this, h, m)
            AlarmScheduler.scheduleDailyAlarm(this, h, m)
        }, hour, minute, true).show()
    }

    private fun showReminderDaysDialog() {
        val currentDays = ReminderManager.getReminderDays(this)
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentDays.toString())
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Set Reminder Days")
            .setMessage("Enter days before expiry for notification:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val entered = input.text.toString().toIntOrNull()
                if (entered != null && entered in 1..365) {
                    // 1) persist
                    ReminderManager.setReminderDays(this, entered)

                    // 2) cancel existing alarm and reschedule (so the intent sent will contain the new value)
                    val (hour, minute) = ReminderManager.getAlarmTime(this)
                    AlarmScheduler.cancelDailyAlarm(this)
                    AlarmScheduler.scheduleDailyAlarm(this, hour, minute)

                    Toast.makeText(this, "Reminder set to $entered days before expiry", Toast.LENGTH_SHORT).show()
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
        fabMain = findViewById(R.id.fabMain)
        fabAdd = findViewById(R.id.fabAdd)
        fabSetAlarm = findViewById(R.id.fabSetAlarm)
        fabSetReminder = findViewById(R.id.fabSetReminder)

        fabMain.setOnClickListener { toggleFabMenu() }
        fabAdd.setOnClickListener { showSimCardDialog(false); collapseFabMenu() }
        fabSetAlarm.setOnClickListener { openTimePickerDialog(); collapseFabMenu() }
        fabSetReminder.setOnClickListener { showReminderDaysDialog(); collapseFabMenu() }
    }

    private fun toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen
        val visibility = if (isFabMenuOpen) android.view.View.VISIBLE else android.view.View.GONE
        fabAdd.visibility = visibility
        fabSetAlarm.visibility = visibility
        fabSetReminder.visibility = visibility
    }

    private fun collapseFabMenu() {
        isFabMenuOpen = false
        fabAdd.visibility = android.view.View.GONE
        fabSetAlarm.visibility = android.view.View.GONE
        fabSetReminder.visibility = android.view.View.GONE
    }

    // --- Theme Toggle ---
    private fun setupThemeToggle() {
        fabMain.setOnLongClickListener {
            val options = arrayOf("System Default", "Light", "Dark")
            AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setItems(options) { _, which ->
                    val mode = when (which) {
                        0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        1 -> AppCompatDelegate.MODE_NIGHT_NO
                        2 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    ThemeHelper.setThemeMode(this, mode)
                }.show()
            true
        }
    }

    // --- Permissions ---
    private fun checkNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
