package com.example.keepbalanced.ui.dialog

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.keepbalanced.R
import com.example.keepbalanced.model.CategoryConfig
import com.example.keepbalanced.model.Transaction
import com.google.firebase.auth.auth
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.gson.Gson
import java.util.Calendar
import java.util.Date // <-- Importante

class AddTransactionViewModel : ViewModel() {

    // ... (propiedades de Firebase y LiveData siguen igual) ...
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val remoteConfig = Firebase.remoteConfig
    private val gson = Gson()

    private val _categoryConfig = MutableLiveData<CategoryConfig>()
    val categoryConfig: LiveData<CategoryConfig> = _categoryConfig

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _dismissDialog = MutableLiveData<Boolean>(false)
    val dismissDialog: LiveData<Boolean> = _dismissDialog

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage


    init {
        setupRemoteConfig()
        fetchCategories()
    }

    private fun setupRemoteConfig() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 10
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    fun fetchCategories() {
        _isLoading.value = true
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    val jsonConfig = remoteConfig.getString("categorias_json")
                    if (jsonConfig.isNotEmpty()) {
                        try {
                            val config = gson.fromJson(jsonConfig, CategoryConfig::class.java)
                            _categoryConfig.value = config
                        } catch (e: Exception) {
                            _errorMessage.value = "Error al procesar JSON: ${e.message}"
                        }
                    } else {
                        _errorMessage.value = "JSON de categorías vacío en Remote Config."
                    }
                } else {
                    _errorMessage.value = "Error al cargar configuración remota."
                }
            }
    }

    /**
     * Guarda una nueva transacción en Firestore.
     * --- CAMBIO AQUÍ: Añadimos 'fecha: Date' como parámetro ---
     */
    fun saveTransaction(tipo: String, monto: Double, categoria: String, subcategoria: String?, fecha: Date) {
        _isLoading.value = true

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "Error: Usuario no autenticado."
            _isLoading.value = false
            return
        }

        // --- CAMBIO AQUÍ ---
        // Usamos la fecha proporcionada para calcular el mes y el año
        val calendario = Calendar.getInstance()
        calendario.time = fecha // Establecemos el calendario en la fecha seleccionada

        val mes = calendario.get(Calendar.MONTH) + 1 // +1 porque Enero es 0
        val anio = calendario.get(Calendar.YEAR)

        // Crear el objeto Transacción
        val transaccion = Transaction(
            usuarioId = userId,
            tipo = tipo.lowercase(),
            monto = monto,
            categoria = categoria,
            subcategoria = subcategoria,
            mes = mes,
            anio = anio,
            fecha = fecha
        )

        db.collection("transacciones")
            .add(transaccion)
            .addOnSuccessListener {
                Log.d("AddTransactionVM", "Transacción guardada con éxito.")
                _isLoading.value = false
                _dismissDialog.value = true
            }
            .addOnFailureListener { e ->
                Log.e("AddTransactionVM", "Error al guardar transacción", e)
                _isLoading.value = false
                _errorMessage.value = "Error al guardar: ${e.message}"
            }
    }
}