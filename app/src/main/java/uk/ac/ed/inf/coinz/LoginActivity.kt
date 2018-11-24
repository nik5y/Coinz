package uk.ac.ed.inf.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login2.*

class LoginActivity : AppCompatActivity() {

    private val tag = "LoginActivity"

    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login2)

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

        mAuth.signInWithEmailAndPassword(email,password)
        //add failure and all that.
    }





}
