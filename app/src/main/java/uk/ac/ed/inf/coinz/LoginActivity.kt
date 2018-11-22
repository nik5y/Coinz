package uk.ac.ed.inf.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login2.*

class LoginActivity : AppCompatActivity() {

    private val tag = "LoginActivity"

    private lateinit var mAuth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login2)

        mAuth = FirebaseAuth.getInstance()

        loginNotRegistered.setOnClickListener {
            goToSignup()
        }

    }

    fun goToSignup() {
        val intent : Intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
    }





}
