package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.CategoryDao
import com.example.myapplication.data.Expense
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // Database and DAO instances for accessing expense/category data. View Binding for layout access.
    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var binding: ActivityMainBinding

    // Variables to hold user input: amount, category, and user ID. Calendar for date picker.
    private var selectedAmount: Int = 0
    private lateinit var selectedCategory: String
    private var userId: Int = 0
    private val calendar: Calendar = Calendar.getInstance()

    // --- Image Handling ---
    // Temporary URI for camera output. Final URI for persistently saved image.
    private var currentPhotoUri: Uri? = null
    private var savedImageUriString: String? = null

    // --- Activity Result Launchers ---
    // Launchers for gallery, camera, and permission requests.
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>

    // --- Activity Lifecycle ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate layout, initialize DB and DAO.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)
        categoryDao = db.categoryDao()

        // Retrieve and validate User ID. Finish if invalid.
        userId = intent.getIntExtra("USER_ID", 0)
        if (userId == 0) {
            Toast.makeText(this, "Invalid user session. Cannot add expense.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize Activity Result Launchers. Configure UI and listeners.
        setupResultLaunchers()
        setupToolbar()
        setupCategorySpinner()
        setupSeekBar()
        setupDatePicker()
        setupAddCategoryButton()
        setupAddImageButton()
        setupSaveButton()
    }

    /** Refreshes the category spinner when the activity resumes. */
    override fun onResume() {
        super.onResume()
        refreshCategorySpinner()
    }

    /** Initializes ActivityResultLaunchers for gallery, camera, and permissions. */
    private fun setupResultLaunchers() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleSelectedImage(it) }
        }
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            currentPhotoUri?.let { uri -> // Check if we have a URI first
                if (success) {
                    handleSelectedImage(uri)
                } else {
                    Toast.makeText(this, "Image capture failed or cancelled", Toast.LENGTH_SHORT).show()
                    currentPhotoUri = null // Reset the URI on failure
                }
            } ?: run { // Handle the case where currentPhotoUri is unexpectedly null
                Toast.makeText(this, "Error: Captured image URI is null", Toast.LENGTH_SHORT).show()
            }
        }
        requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) { launchCamera() } else {
                Toast.makeText(this, "Camera permission denied. Cannot take photo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Configures the Toolbar with title and back button. */
    private fun setupToolbar() {
        binding.toolBarHome.setNavigationOnClickListener { finish() }
    }

    /** Initiates loading categories into the spinner asynchronously. */
    private fun setupCategorySpinner() {
        lifecycleScope.launch { refreshCategorySpinner() }
    }

    /** Configures the SeekBar for selecting the expense amount. */
    private fun setupSeekBar() {
        binding.seekAmount.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedAmount = progress
                binding.tvAmount.text = "Amount: R $selectedAmount" // TODO: Use string resources
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { /* No action needed */ }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { /* No action needed */ }
        })
        selectedAmount = binding.seekAmount.progress
        binding.tvAmount.text = "Amount: R $selectedAmount" // TODO: Use string resources
    }

    /** Configures the EditText to trigger the DatePickerDialog on click. */
    private fun setupDatePicker() {
        binding.editTextText.setOnClickListener { showDatePicker() }
        updateDateEditText()
    }

    /** Sets up the click listener for the "Add Category" button. */
    private fun setupAddCategoryButton() {
        binding.btnAddCategory.setOnClickListener {
            val intent = Intent(this, AddCategoryActivity::class.java).putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }

    /** Sets up the click listener for the "Add Image" button to show image source options. */
    private fun setupAddImageButton() {
        binding.btnAddImage.setOnClickListener { showImageSourceDialog() }
    }

    /** Sets up the click listener for the "Save Expense" button, performing validation and data saving. */
    private fun setupSaveButton() {
        binding.btnSaveExpense.setOnClickListener {
            val description = binding.editTextTextMultiLine.text.toString().trim()
            val date = binding.editTextText.text.toString()

            if (description.isBlank() || date.isBlank() || !::selectedCategory.isInitialized || selectedCategory.isBlank()) {
                Toast.makeText(this, "Please fill description, date, and select a category.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (selectedAmount <= 0) {
                Toast.makeText(this, "Please select an amount greater than zero.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Adds expense to database
            val newExpense = Expense(
                amount = selectedAmount.toDouble(),
                category = selectedCategory,
                description = description,
                date = date,
                userId = userId,
                imageUri = savedImageUriString
            )
            setResult(Activity.RESULT_OK, Intent().putExtra("NEW_EXPENSE", newExpense))
            finish()
        }
    }

    /** Processes the URI of a selected image, saving it to internal storage. */
    private fun handleSelectedImage(uri: Uri) {
        lifecycleScope.launch {
            val internalUri = saveImageToInternalStorage(uri)
            if (internalUri != null) {
                savedImageUriString = internalUri.toString()
                binding.ivExpenseImagePreview.setImageURI(internalUri)
                binding.ivExpenseImagePreview.visibility = View.VISIBLE
                Toast.makeText(this@MainActivity, "Image attached successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to save image to internal storage", Toast.LENGTH_SHORT).show()
                savedImageUriString = null
                binding.ivExpenseImagePreview.visibility = View.GONE
                binding.ivExpenseImagePreview.setImageURI(null)
            }
        }
    }

    /** Saves an image from a source URI to the app's internal storage. */
    private suspend fun saveImageToInternalStorage(sourceUri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            var inputStream: java.io.InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = contentResolver.openInputStream(sourceUri)
                val filename = "expense_${UUID.randomUUID()}.jpg"
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE)
                inputStream?.copyTo(outputStream)
                Uri.fromFile(getFileStreamPath(filename))
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("MainActivity", "Error saving image to internal storage", e)
                null
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        }
    }

    /** Shows a dialog to choose between taking a photo or selecting from the gallery. */
    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Add Expense Image")
            .setItems(arrayOf("Take Photo", "Choose from Gallery", "Cancel")) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> launchGallery()
                    2 -> dialog.dismiss()
                }
            }
            .setCancelable(true)
            .show()
    }

    /** Launches the system's gallery picker. */
    private fun launchGallery() {
        pickImageLauncher.launch("image/*")
    }

    /** Checks camera permission and launches the camera if granted. */
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Camera Permission Needed")
                    .setMessage("To attach a photo of your expense receipt or item, please grant camera access.")
                    .setPositiveButton("Grant") { _, _ -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /** Creates a temporary file URI and launches the camera app. */
    private fun launchCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: Exception) {
            android.util.Log.e("MainActivity", "Error creating image file for camera", ex)
            Toast.makeText(this, "Could not create file for photo", Toast.LENGTH_SHORT).show()
            null
        }

        // Use let with a non-nullable temporary variable for smart casting
        photoFile?.let { nonNullPhotoFile ->
            takePictureLauncher.launch(FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", nonNullPhotoFile))
        }
        // If photoFile is null, this block is skipped, and the camera won't launch.
    }

    /** Creates a uniquely named temporary image file. */
    @Throws(java.io.IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // --- UI Update & Data Loading Helpers ---
    /** Updates the date EditText with the selected date. */
    private fun updateDateEditText() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.editTextText.setText(sdf.format(calendar.time))
    }

    /** Fetches categories and populates the spinner asynchronously. Handles empty category list. */
    private fun refreshCategorySpinner() {
        lifecycleScope.launch {
            val categories = getCategoriesList(userId)

            if (categories.isNotEmpty()) {
                if (!::selectedCategory.isInitialized) selectedCategory = categories[0]
            } else {
                Toast.makeText(this@MainActivity, "No categories found. Please add one first.", Toast.LENGTH_LONG).show()
                if (::selectedCategory.isInitialized) { /* Potentially clear selectedCategory */ }
            }

            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinner.adapter = adapter

            if (::selectedCategory.isInitialized) {
                val position = categories.indexOf(selectedCategory)
                if (position >= 0) {
                    binding.spinner.setSelection(position)
                } else if (categories.isNotEmpty()){
                    selectedCategory = categories[0]
                    binding.spinner.setSelection(0)
                }
            }

            binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    if (categories.isNotEmpty()) selectedCategory = categories[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    if (categories.isNotEmpty()) { /* selectedCategory = categories[0] */ }
                }
            }
        }
    }

    /** Shows the DatePickerDialog for selecting a date. */
    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(Calendar.YEAR, selectedYear)
                    calendar.set(Calendar.MONTH, selectedMonth)
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                    updateDateEditText()
                },
                year, month, day)
        datePickerDialog.show()
    }

    /** Fetches the list of category names for a user from the database. */
    private suspend fun getCategoriesList(userId: Int): List<String> {
        return withContext(Dispatchers.IO) { categoryDao.getAllNamesForUser(userId) }
    }
}