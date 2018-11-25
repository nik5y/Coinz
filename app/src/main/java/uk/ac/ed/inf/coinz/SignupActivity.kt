package uk.ac.ed.inf.coinz

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_signup.*
import java.net.URI
import java.util.*

class SignupActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()

    private val tag = "SignupActivity"

    private var profilePicture: Uri? = null

    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        //Register:
        registerButton.setOnClickListener {
            doRegistration()
        }

        //Login instead:
        registerAlreadyRegistered.setOnClickListener {
            goToLogin()
        }

        //photo
        registerPicture.setOnClickListener {

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(tag, "Photo chosen successfully.")

            profilePicture = data.data

            val image_bmp = MediaStore.Images.Media.getBitmap(contentResolver, profilePicture)

            registerPicture.setBackgroundDrawable(BitmapDrawable(image_bmp))

        }

    }

    //Go to Login Activity:

    private fun goToLogin() {
        val intent: Intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    //Register:

    private fun doRegistration() {

        val username: String = registerUsername.text.toString()
        val email: String = registerEmail.text.toString()
        val password: String = registerPassword.text.toString()
        val passwordConfirm: String = registerConfirmPassword.text.toString()

        //Firebase Auth tings:------------------------

        var alert = AlertDialog.Builder(this)
        alert.apply {
            //setTitle("Error:")
            setPositiveButton("OK", null)
            setCancelable(true)
            create()
        }

        if (email.isEmpty() || password.isEmpty() || username.isEmpty() || passwordConfirm.isEmpty()) {
            alert.setMessage("Please fill out all of the forms.")
            alert.show()
            Log.d(tag, "Forms left empty.")
            return
        }

        if (password == passwordConfirm) {

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        if (!it.isSuccessful) return@addOnCompleteListener
                        Log.d(tag, "Created user with ID ${it.result?.user?.uid}")
                        mAuth.signInWithEmailAndPassword(email, password)
                        uploadProfilePicture()
                        addUserToDatabase(username, email)
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
            registerPassword.setText("")
            registerPassword.setSelection(0)
            registerConfirmPassword.setText("")
        }

    }

    //Go to Maps:

    private fun goToMaps() {
        startActivity(Intent(this, MapsActivity::class.java))
    }

    //Upload Picture to Firebase:

    private fun uploadProfilePicture() {

        if (profilePicture == null) return
        //Use the profile pic identification as the users uid, as a user can only have one profile pic at a time:
        val name = mAuth.currentUser?.uid
        val reference = FirebaseStorage.getInstance().getReference("/Pictures/$name")
        reference.putFile(profilePicture!!).addOnSuccessListener {
            Log.d(tag, "Profile picture uploaded successfully.")
        }

    }

    //Add user to Firebase:

    private fun addUserToDatabase(username: String, email: String) {
        //  reference.setValue()

        val name = mAuth.currentUser?.uid
        val reference = FirebaseStorage.getInstance().getReference("/Pictures/$name")

        val userReference = firestore.collection(username).document("Personal Information")
        if(profilePicture==null) {
            userReference.set(User(email)).addOnCompleteListener {
                if (!it.isSuccessful) {
                    Log.d(tag, "User NOT added to the Database!")
                } else {
                    Log.d(tag, "User Succesfully added to the Database. No Uploaded Picture.")
                }
            }
        } else {
            userReference.set(User(email,reference.downloadUrl.toString())).addOnCompleteListener {
                if (!it.isSuccessful) {
                    Log.d(tag, "User NOT added to the Database!")
                } else {
                    Log.d(tag, "User Succesfully added to the Database")
                }
            }
        }
    }


}


class User(var email:String, var pictureURL:String="Not Uploaded")


