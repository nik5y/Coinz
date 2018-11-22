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

    private lateinit var mAuth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        //Collect input:--------------------------

        registerButton.setOnClickListener {
            val username : String = registerUsername.text.toString()
            val email : String = registerEmail.text.toString()
            val password : String = registerPassword.text.toString()
            val passwordConfirm : String = registerConfirmPassword.text.toString()

            Log.d(tag, "Email is $email"  )
            Log.d(tag, "Password is $password"  )
            Log.d(tag, "Password Confirm is $passwordConfirm"  )

            //Firebase Auth tings:------------------------

            var alert = AlertDialog.Builder(this)
            alert.apply {
                setTitle("Error...")
                setPositiveButton("OK", null)
                setCancelable(true)
                create()
            }

            if (email.isEmpty()&&password.isEmpty()) {
                alert.setMessage("Please enter email and password")
                alert.show()
                return@setOnClickListener

            } else if (password.isEmpty()) {

                alert.setMessage("Please enter password")
                alert.show()
                return@setOnClickListener

            } else if (password.isEmpty()) {

                alert.setMessage("passwords do not match")
                alert.show()
                return@setOnClickListener

            } else if (username.isEmpty()) {

                alert.setMessage("Please enter password")
                alert.show()

            }else if (password == passwordConfirm) {

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (!it.isSuccessful) return@addOnCompleteListener

                    Log.d(tag, "Created user  ${it.result?.user?.uid}")
                }
            } else {
                val alert = AlertDialog.Builder(this)

                alert.setMessage("passwords do not match")
                alert.show()

                registerPassword.setText("")
                registerConfirmPassword.setText("")
            }

        }

        //Login instead:---------------------------
        registerAlreadyRegistered.setOnClickListener{
            goToLogin()
        }



    }

    fun goToLogin() {
        val intent : Intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }





}
