package com.example.keepbalanced

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializar Firebase Auth
        auth = Firebase.auth

        // Enlazar vistas
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnRegister = findViewById(R.id.btn_register)
        progressBar = findViewById(R.id.progress_bar)

        btnRegister.setOnClickListener {
            performRegistration()
        }
    }

    private fun performRegistration() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validaciones
        if (email.isEmpty()) {
            etEmail.error = "El correo es obligatorio"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo no válido"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "La contraseña es obligatoria"
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            etPassword.requestFocus()
            return
        }

        // Mostrar ProgressBar y deshabilitar botón
        progressBar.visibility = View.VISIBLE
        btnRegister.isEnabled = false

        // Crear usuario con Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                btnRegister.isEnabled = true

                if (task.isSuccessful) {
                    // Registro exitoso, vamos a la app principal
                    Toast.makeText(baseContext, "Registro exitoso.", Toast.LENGTH_SHORT).show()

                    // Aquí podríamos crear un documento inicial en Firestore para el usuario si quisiéramos

                    val intent = Intent(this, MainActivity::class.java)
                    // Limpiamos la pila de actividades para que no pueda volver atrás
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    // Si falla el registro, mostramos un mensaje.
                    Toast.makeText(baseContext, "Fallo en el registro: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }
}