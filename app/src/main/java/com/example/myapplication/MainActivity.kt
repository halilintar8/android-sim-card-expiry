package com.example.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
import com.example.myapplication.data.SimCard
import com.example.myapplication.data.SimCardDatabase
import com.example.myapplication.worker.AlarmScheduler
import com.example.myapplication.worker.SimExpiryReceiver
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SimCardAdapter
    private val simCardList = mutableListOf<SimCard>()

    private val simCardDao by lazy { SimCardDatabase.getDatabase(this).simCardDao() }

    // Toggle test mode for immediate run
    private val isTesting = false

    // Alarm time (24-hour format)
    private val ALARM_HOUR = 7
    private val ALARM_MINUTE = 0

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

        createNotificationChannel()
        checkNotificationPermission()

        setupRecyclerView()
        observeSimCards()

        if (isTesting) {
            // Trigger immediately for debug
            AlarmScheduler.scheduleExactAlarmInSeconds(this, 5)
        } else {
            // Schedule daily alarm
            AlarmScheduler.scheduleDailyAlarm(this, ALARM_HOUR, ALARM_MINUTE)
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showSimCardDialog(isEdit = false)
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
            Log.d(TAG, "Sim list size=${simCardList.size}")
        })
    }

    private fun showSimCardDialog(isEdit: Boolean, position: Int = -1) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sim_card, null)
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
                                    id = 0, // Room will auto-generate ID
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

    companion object {
        private const val TAG = "MainActivity"
    }
}
