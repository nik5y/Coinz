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

        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener {
            if(!it.isSuccessful) return@addOnCompleteListener
            Log.d(tag, "Login Successful.")
            goToMap()
        }.addOnFailureListener {
            var alert = AlertDialog.Builder(this)
            alert.apply {
                setMessage("${it.message}")
                setPositiveButton("OK", null)
                setCancelable(true)
                create().show()
            }
            Log.d(tag, "Login Failed. ${it.message}")
        }
        //add failure and all that.
    }

    private fun goToMap() {
        startActivity(Intent(this, MapsActivity::class.java))
    }

    override fun onStop() {
        super.onStop()
        loginEmail.setText("")
        loginEmail.setSelection(0)
        loginPassword.setText("")
    }
}
