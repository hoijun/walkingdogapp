package com.example.walkingdogapp

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.walkingdogapp.databinding.ActivitySettingDogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SettingDogActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingDogBinding
    private val doginfo = DogInfo()
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.database

    private val BackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectgoHome()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingDogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.onBackPressedDispatcher.addCallback(this, BackPressCallback)

        binding.apply {
            btnDogismale.setOnClickListener {
                doginfo.gender = btnDogismale.text.toString()
                btnDogismale.setBackgroundColor(Color.DKGRAY)
                btnDogisfemale.setBackgroundColor(Color.GRAY)
            }
            btnDogisfemale.setOnClickListener {
                doginfo.gender = btnDogisfemale.text.toString()
                btnDogisfemale.setBackgroundColor(Color.DKGRAY)
                btnDogismale.setBackgroundColor(Color.GRAY)
            }

            btnNeuteryes.setOnClickListener {
                doginfo.neutering = btnNeuteryes.text.toString()
                btnNeuteryes.setBackgroundColor(Color.DKGRAY)
                btnNeuterno.setBackgroundColor(Color.GRAY)
            }
            btnNeuterno.setOnClickListener {
                doginfo.neutering = btnNeuterno.text.toString()
                btnNeuterno.setBackgroundColor(Color.DKGRAY)
                btnNeuteryes.setBackgroundColor(Color.GRAY)
            }

            btnVaccyes.setOnClickListener {
                doginfo.vaccination = btnVaccyes.text.toString()
                btnVaccyes.setBackgroundColor(Color.DKGRAY)
                btnVaccno.setBackgroundColor(Color.GRAY)
            }
            btnVaccno.setOnClickListener {
                doginfo.vaccination = btnVaccno.text.toString()
                btnVaccno.setBackgroundColor(Color.DKGRAY)
                btnVaccyes.setBackgroundColor(Color.GRAY)
            }

            editBreed.setOnClickListener {

            }

            editBirth.setOnClickListener {

            }

            registerDog.setOnClickListener {
                doginfo.apply {
                    if (editName.text.toString() == "" || editBreed.text == "" || editBirth.text == "" || editWeight.text.toString() == ""
                        || gender == "" || neutering == "" || vaccination == "") {
                        val builder = AlertDialog.Builder(this@SettingDogActivity)
                        builder.setTitle("빈칸이 남아있어요.")
                        builder.setPositiveButton("확인", null)
                        builder.show()
                        return@setOnClickListener
                    }

                    val builder = AlertDialog.Builder(this@SettingDogActivity)
                    builder.setTitle("등록 할까요?")
                    val listener = DialogInterface.OnClickListener { _, ans ->
                        when (ans) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                val user = auth.currentUser?.uid
                                val userRef = db.getReference("Users")
                                userRef.child("$user").child("dog").setValue(doginfo)
                                goHome()
                            }
                        }
                    }
                    builder.setPositiveButton("네", listener)
                    builder.setNegativeButton("아니요", null)
                    builder.show()
                    return@setOnClickListener
                }
            }

            btnBack.setOnClickListener {
                selectgoHome()
            }
        }
    }

    private fun selectgoHome() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("나가시겠어요?")
        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                   goHome()
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니요", null)
        builder.show()
    }

    private fun goHome() {
        val backIntent = Intent(this, MainActivity::class.java)
        backIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(backIntent)
        finish()
    }
}