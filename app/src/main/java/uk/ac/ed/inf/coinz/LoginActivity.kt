package uk.ac.ed.inf.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
//
//
class LoginActivity : AppCompatActivity() {

    private val tag = "LoginActivity"

    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginNotRegistered.setOnClickListener {
            goToSignup()
        }

        loginButton.setOnClickListener {
            doLogin()
        }

    }

    //Go to SignUp
    private fun LoginActivity.goToSignup() {
        val intent : Intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
    }

    //Login
    private fun doLogin() {

        val email = loginEmail.text.toString()
        val password = loginPassword.text.toString()
        val alert = AlertDialog.Builder(this)
        alert.apply {

            setPositiveButton("OK", null)
            setCancelable(true)
            create()
        }

        if (email.isEmpty() || password.isEmpty()) {
            alert.apply {
                setMessage("Please fill out the forms.")
                .show()
            }
            Log.d(tag, "Invalid input")
            return
        } else {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                Log.d(tag, "Login Successful. Going to MapsActivity")
                goToMap()
            }.addOnFailureListener {
                alert.apply {
                    setMessage("${it.message}")
                    show()
                }
                Log.d(tag, "Login Failed. ${it.message}")
            }
        }
    }

    private fun goToMap() {
        var intent = Intent(this, MapsActivity::class.java)
        //as we already have a signout button, when taping the 'back' button at bottom left, the screen just goes to home screen
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onStop() {
        super.onStop()
        loginEmail.setText("")
        loginEmail.setSelection(0)
        loginPassword.setText("")
    }
}
