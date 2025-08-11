package com.halilintar8.simexpiry

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
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
import com.halilintar8.simexpiry.data.SimCard
import com.halilintar8.simexpiry.data.SimCardDatabase
import com.halilintar8.simexpiry.worker.AlarmScheduler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.halilintar8.simexpiry.R
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SimCardAdapter
    private val simCardList = mutableListOf<SimCard>()

    private val simCardDao by lazy { SimCardDatabase.getDatabase(this).simCardDao() }

    private lateinit var sharedPrefs: android.content.SharedPreferences

    // Default alarm time (7:00 AM)
    private var alarmHour = 7
    private var alarmMinute = 0

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

        Log.d(TAG, "onCreate called")

        sharedPrefs = getSharedPreferences("sim_prefs", Context.MODE_PRIVATE)
        alarmHour = sharedPrefs.getInt("alarm_hour", alarmHour)
        alarmMinute = sharedPrefs.getInt("alarm_minute", alarmMinute)

        createNotificationChannel()
        checkNotificationPermission()

        setupRecyclerView()
        observeSimCards()

        // Schedule alarm for saved time on app start
        AlarmScheduler.scheduleDailyAlarm(this, alarmHour, alarmMinute)

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showSimCardDialog(isEdit = false)
        }

        findViewById<Button>(R.id.btnSetAlarmTime).setOnClickListener {
            openTimePickerDialog()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        adapter = SimCardAdapter(
            simCardList,
            onEditClick = { pos -> showSimCardDialog(isEdit = true, position = pos) },
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
            Log.d(TAG, "Sim list updated, size=${simCardList.size}")
        })
    }

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

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit SIM Card" else "Add SIM Card")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                val name = etName.text.toString().trim()
                val simNumber = etSimNumber.text.toString().trim()
                val expiredDate = etExpiredDate.text.toString().trim()

                if (name.isNotEmpty() && simNumber.isNotEmpty() && expiredDate.isNotEmpty()) {
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
                } else {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSimCard(position: Int) {
        lifecycleScope.launch {
            simCardDao.delete(simCardList[position])
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "sim_expiry_channel"
            val channelName = "SIM Expiry Alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notifications for SIM card expiry alerts"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun openTimePickerDialog() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                alarmHour = selectedHour
                alarmMinute = selectedMinute

                // Save to SharedPreferences
                sharedPrefs.edit()
                    .putInt("alarm_hour", alarmHour)
                    .putInt("alarm_minute", alarmMinute)
                    .apply()

                Toast.makeText(
                    this,
                    "Notification time set to %02d:%02d".format(alarmHour, alarmMinute),
                    Toast.LENGTH_SHORT
                ).show()

                // Reschedule the alarm
                AlarmScheduler.scheduleDailyAlarm(this, alarmHour, alarmMinute)
            },
            alarmHour,
            alarmMinute,
            true
        )
        timePickerDialog.show()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
