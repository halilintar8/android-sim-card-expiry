package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.SimCard
import com.example.myapplication.data.SimCardDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SimCardAdapter
    private val simCardList = mutableListOf<SimCard>()

    // DAO reference
    private val simCardDao by lazy {
        SimCardDatabase.getDatabase(this).simCardDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)

        adapter = SimCardAdapter(
            simCardList,
            onEditClick = { position -> showEditDialog(position) },
            onDeleteClick = { position -> deleteSimCard(position) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // ✅ Observe LiveData from the database
        simCardDao.getAllSimCards().observe(this, Observer { simCards ->
            simCardList.clear()
            simCardList.addAll(simCards)
            adapter.notifyDataSetChanged()
        })

        fabAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sim_card, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etSimNumber = dialogView.findViewById<EditText>(R.id.etSimNumber)
        val etExpiredDate = dialogView.findViewById<EditText>(R.id.etExpiredDate)

        AlertDialog.Builder(this)
            .setTitle("Add SIM Card")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val simNumber = etSimNumber.text.toString().trim()
                val expiredDate = etExpiredDate.text.toString().trim()

                if (name.isNotEmpty() && simNumber.isNotEmpty() && expiredDate.isNotEmpty()) {
                    lifecycleScope.launch {
                        simCardDao.insert(SimCard(name = name, simCardNumber = simNumber, expiredDate = expiredDate))
                    }
                } else {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(position: Int) {
        val simCard = simCardList[position]
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sim_card, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etSimNumber = dialogView.findViewById<EditText>(R.id.etSimNumber)
        val etExpiredDate = dialogView.findViewById<EditText>(R.id.etExpiredDate)

        etName.setText(simCard.name)
        etSimNumber.setText(simCard.simCardNumber)
        etExpiredDate.setText(simCard.expiredDate)

        AlertDialog.Builder(this)
            .setTitle("Edit SIM Card")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedName = etName.text.toString().trim()
                val updatedSimNumber = etSimNumber.text.toString().trim()
                val updatedExpiredDate = etExpiredDate.text.toString().trim()

                if (updatedName.isNotEmpty() && updatedSimNumber.isNotEmpty() && updatedExpiredDate.isNotEmpty()) {
                    lifecycleScope.launch {
                        simCardDao.update(
                            simCard.copy(
                                name = updatedName,
                                simCardNumber = updatedSimNumber,
                                expiredDate = updatedExpiredDate
                            )
                        )
                    }
                } else {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSimCard(position: Int) {
        val simCard = simCardList[position]
        lifecycleScope.launch {
            simCardDao.delete(simCard)
        }
    }
}
