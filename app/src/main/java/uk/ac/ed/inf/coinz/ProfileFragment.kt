@file:SuppressLint("LogNotTimber")

package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*

@Suppress("DEPRECATION")
class ProfileFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val email = FirebaseAuth.getInstance().currentUser!!.email.toString()
    private var profilePicture : Uri? = null

    @SuppressLint("ResourceType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        fetchAndDisplayInfo(view)

        view.profile_picture.setOnClickListener {

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 66)
        }


        //allows the user to choose another profile picture. method implemented similarly as in signupActivity
        view.profile_picture_confirm.setOnClickListener {

            val name = FirebaseAuth.getInstance().currentUser!!.uid
            val reference = FirebaseStorage.getInstance().getReference("/Pictures/$name")
            reference.putFile(profilePicture!!).addOnSuccessListener { _->

                Log.d(tag, "Profile picture uploaded successfully.")
                reference.downloadUrl.addOnSuccessListener { url ->

                    firestore.collection("Users").document(email).collection("Account Information")
                            .document("Personal Details").update("pictureURL", url.toString())

                }
            }
            profile_picture_confirm.visibility = View.GONE
        }

        return view

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 66 && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(tag, "Photo chosen successfully.")

            profilePicture = data.data

            val imageBmp = MediaStore.Images.Media.getBitmap(activity!!.contentResolver, profilePicture)

            //display the newly selected picture

            profile_picture.setImageBitmap(imageBmp)

            //allow for the user to confirm the picture selection and store it in the Database and Storage

            profile_picture_confirm.visibility = View.VISIBLE

        }

    }

    @SuppressLint("SetTextI18n")
    private fun fetchAndDisplayInfo(view: View) {

        val userReference = firestore.collection("Users").document(email)
                .collection("Account Information")

        userReference.document("Personal Details").get().addOnSuccessListener { details ->

                    val username = details["username"] as String
                    val pictureURL = details["pictureURL"] as String

                    //although displaying email inside here is unnecessary, it was done so it looks simultaneous

                    view.profile_email_value.text = email

                    view.profile_username_value.text = "$username's Stats"

                    if (pictureURL != "Not Uploaded") {
                        //using the picasso library for image cashing
                        Picasso.get().load(pictureURL).into(view.profile_picture)
                    } else {
                        view.profile_picture.setImageDrawable(resources.getDrawable(R.drawable.peny))
                    }
                }

        userReference.document("Gold Balance").get().addOnSuccessListener { gold->
            val goldBalance = gold["goldBalance"].toString().toDouble()
            if (goldBalance.format(3) == "0.000") {
                view.profile_gold_value.text = "0" //for prettiness
            } else {
                view.profile_gold_value.text = goldBalance.format(3)
            }

            //Setting up the level up button, which utilises users gold balance as well:

            userReference.document("Level").get().addOnSuccessListener { level ->
                //displaying level
                val lvl = level["level"].toString()
                view.profile_level_value.text = lvl

                //displaying the cost and that of the new level

                val levelPrice = LevelingSystem().levelPrice(lvl.toInt())

                if (levelPrice == 1010101.0) {
                    view.profile_levelup.isEnabled = false
                    view.profile_levelup.setBackgroundColor(resources.getColor(R.color.vikaLightOrange))
                    view.profile_levelup.text = "Max Level"
                } else {

                    view.profile_levelup.text = "Level up for ${levelPrice.toString().substringBefore(".")} GOLD"

                    view.profile_levelup.setOnClickListener {

                        if (levelPrice <= goldBalance) {

                            //update level
                            userReference.document("Level").update("level", lvl.toInt() + 1)

                            //tale away from gold balance
                            userReference.document("Gold Balance").update("goldBalance", goldBalance - levelPrice)

                            //update views

                            fragmentManager!!.beginTransaction().detach(this).attach(this).commit()

                        } else {
                            val alert = AlertDialog.Builder(context)

                            alert.apply {
                                setPositiveButton("OK", null)
                                setCancelable(true)
                                setMessage("Not enough gold!")
                                create().show()
                            }

                        }

                    }

                }
            }

        }

        //retrieve banked count

        userReference.document("Banked Coin Counter").get().addOnSuccessListener { counter->
            view.profile_banked_value.text = counter["count"].toString()
        }

        //retrieve sent collected count

        userReference.document("Collected Coin Counter").get().addOnSuccessListener { counter->
            view.profile_collected_value.text = counter["count"].toString()
        }



    }
}