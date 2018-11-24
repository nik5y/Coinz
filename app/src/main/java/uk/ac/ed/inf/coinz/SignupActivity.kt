package uk.ac.ed.inf.coinz

import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_signup.*

class SignupActivity : AppCompatActivity() {

    private val tag = "SignupActivity"

    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        //Register:
        registerButton.setOnClickListener {
            doRegistration()
        }

        //Login instead:
        registerAlreadyRegistered.setOnClickListener{
            goToLogin()
        }

    }

    //Go to Login Activity:

    private fun goToLogin() {
        val intent : Intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    //Register:

    private fun doRegistration() {

        val username : String = registerUsername.text.toString()
        val email : String = registerEmail.text.toString()
        val password : String = registerPassword.text.toString()
        val passwordConfirm : String = registerConfirmPassword.text.toString()

        //Firebase Auth tings:------------------------

        var alert = AlertDialog.Builder(this)
        alert.apply {
            //setTitle("Error:")
            setPositiveButton("OK", null)
            setCancelable(true)
            create()
        }

        if ( email.isEmpty() || password.isEmpty() || username.isEmpty() || passwordConfirm.isEmpty()) {
            alert.setMessage("Please fill out all of the forms.")
            alert.show()
            Log.d(tag, "Forms left empty.")
            return
        }

        if (password == passwordConfirm) {

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        if (!it.isSuccessful) return@addOnCompleteListener
                        Log.d(tag, "Created user  ${it.result?.user?.uid}")
                        mAuth.signInWithEmailAndPassword(email,password)
                        goToMaps()
                    }.addOnFailureListener {
                        alert.setMessage("${it.message}")
                        alert.show()
                        Log.d(tag, "Register failed! ${it.message}")
                    }
        } else {
            alert.setMessage("Passwords do not match.")
            alert.show()
            Log.d(tag, "Passwords do not match.")
        }

    }

    //Go to Maps:

    private fun goToMaps() {
        startActivity(Intent(this, MapsActivity::class.java))
    }



}
