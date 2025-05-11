package com.example.fridgemanager1

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fridgeAdapter: FridgeAdapter
    private val itemList = mutableListOf<FridgeItem>()
    private lateinit var dbHelper: FridgeDatabaseHelper
    private lateinit var btnSuggestions: Button
    private lateinit var listViewRecipes: ListView

    private var userPhone: String = ""
    private var userEmail: String = ""
    private val allRecipes = listOf(
        "Grilled Cheese Sandwich",
        "Pasta with Tomato Sauce",
        "Vegetable Stir Fry",
        "Egg Fried Rice",
        "Fruit Salad"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSuggestions = findViewById(R.id.btnSuggestions)
        listViewRecipes = findViewById(R.id.listViewRecipes)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        }

        dbHelper = FridgeDatabaseHelper(this)
        itemList.addAll(dbHelper.getAllItems())

        checkExpiringItems()

        scheduleDailyExpiryCheck()
        showContactDialog()

        recyclerView = findViewById(R.id.fridgeItemsRecyclerView)
        fridgeAdapter = FridgeAdapter(itemList) { position ->
            val item = itemList[position]
            AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(arrayOf("Edit", "Delete")) { _, which ->
                    when (which) {
                        0 -> showEditDialog(position, item)
                        1 -> {
                            if (dbHelper.deleteItem(item.id)) {
                                itemList.removeAt(position)
                                fridgeAdapter.notifyItemRemoved(position)
                                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }.show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fridgeAdapter

        findViewById<FloatingActionButton>(R.id.addItemFab).setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
            val editName = dialogView.findViewById<EditText>(R.id.editItemName)
            val editQuantity = dialogView.findViewById<EditText>(R.id.editItemQuantity)
            val editExpiry = dialogView.findViewById<EditText>(R.id.editItemExpiry)
            val btnAdd = dialogView.findViewById<Button>(R.id.btnAddItem)

            val dialog = AlertDialog.Builder(this).setView(dialogView).create()

            btnAdd.setOnClickListener {
                val name = editName.text.toString()
                val quantity = editQuantity.text.toString()
                val expiry = editExpiry.text.toString()

                if (name.isNotEmpty() && quantity.isNotEmpty() && expiry.isNotEmpty()) {
                    val newItem = FridgeItem(name = name, quantity = quantity, expiryDate = expiry)
                    val insertedId = dbHelper.insertItem(newItem)

                    if (insertedId != -1L) {
                        itemList.add(FridgeItem(insertedId.toInt(), name, quantity, expiry))
                        fridgeAdapter.notifyItemInserted(itemList.size - 1)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Failed to insert item", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.show()
        }

        btnSuggestions.setOnClickListener {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val expiringItems = dbHelper.getAllItems().filter {
                try {
                    val expiry = sdf.parse(it.expiryDate)
                    val diff = (expiry?.time ?: 0) - today.time
                    val daysLeft = diff / (1000 * 60 * 60 * 24)
                    daysLeft in 0..1
                } catch (e: Exception) {
                    false
                }
            }

            val suggestedRecipes = dbHelper.getSuggestedRecipes(expiringItems)
            val recipeNames = suggestedRecipes.map { it.title } // assuming Recipe has a 'name' field
            Log.d("DEBUG", "Suggested recipes: $recipeNames")
            if (suggestedRecipes.isNotEmpty()) {
                val intent = Intent(this, SuggestionsActivity::class.java)
                intent.putStringArrayListExtra("recipes", ArrayList(recipeNames))
                startActivity(intent)

            } else {
                Toast.makeText(this, "No recipe suggestions available", Toast.LENGTH_SHORT).show()
            }
        }


    }

    override fun onResume() {
        super.onResume()
        checkExpiringItems()
    }

    private fun showContactDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_user_contact, null)
        val editPhone = view.findViewById<EditText>(R.id.editPhoneNumber)
        val editEmail = view.findViewById<EditText>(R.id.editEmail)

        AlertDialog.Builder(this)
            .setTitle("Enter Contact Info")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                userPhone = editPhone.text.toString()
                userEmail = editEmail.text.toString()
                Toast.makeText(this, "Contact info saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(position: Int, item: FridgeItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val editName = dialogView.findViewById<EditText>(R.id.editItemName)
        val editQuantity = dialogView.findViewById<EditText>(R.id.editItemQuantity)
        val editExpiry = dialogView.findViewById<EditText>(R.id.editItemExpiry)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddItem)
        btnAdd.text = "Update"

        editName.setText(item.name)
        editQuantity.setText(item.quantity)
        editExpiry.setText(item.expiryDate)

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        btnAdd.setOnClickListener {
            val name = editName.text.toString()
            val quantity = editQuantity.text.toString()
            val expiry = editExpiry.text.toString()

            if (name.isNotEmpty() && quantity.isNotEmpty() && expiry.isNotEmpty()) {
                val success = dbHelper.updateItem(item.id, name, quantity, expiry)
                if (success) {
                    itemList[position] = FridgeItem(item.id, name, quantity, expiry)
                    fridgeAdapter.notifyItemChanged(position)
                    Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun checkExpiringItems() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val expiringTomorrow = itemList.filter {
            try {
                val expiryDate = sdf.parse(it.expiryDate)
                val expiryCal = Calendar.getInstance().apply {
                    time = expiryDate!!
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diff = expiryCal.timeInMillis - today.timeInMillis
                val daysLeft = diff / (1000 * 60 * 60 * 24)
                daysLeft in 0..1
            } catch (e: Exception) {
                false
            }
        }

        if (expiringTomorrow.isNotEmpty()) {
            val message = expiringTomorrow.joinToString("\n") {
                "${it.name} expires on ${it.expiryDate}"
            }

            AlertDialog.Builder(this)
                .setTitle("Item Expiring Tomorrow!")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()

            val phone = if (userPhone.isNotEmpty()) userPhone else "5554"
            sendSMS(phone, message)

            if (userEmail.isNotEmpty()) sendEmail(userEmail, "Fridge Item Expiry", message)
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS sent to $phoneNumber", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail(email: String, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showSuggestionsDialog(suggestedRecipes: List<Recipe>) {
        val recipeTitles = suggestedRecipes.map { it.title }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Suggested Recipes")
            .setItems(recipeTitles) { _, which ->
                val selectedRecipe = suggestedRecipes[which]
                showRecipeDetailsDialog(selectedRecipe)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showRecipeDetailsDialog(recipe: Recipe) {
        AlertDialog.Builder(this)
            .setTitle(recipe.title)
            .setMessage("Ingredients:\n${recipe.ingredients}\n\nSteps:\n${recipe.steps}")
            .setPositiveButton("OK", null)
            .show()
    }


    private fun scheduleDailyExpiryCheck() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ExpiryAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun showSuggestedRecipes() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, allRecipes)
        listViewRecipes.adapter = adapter
    }
}