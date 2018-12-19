package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class GambleFragment : Fragment() {


    @SuppressLint("ResourceType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate( R.layout.fragment_gamble, container, false )
    //todo place bet! so let the user sepcify the amoubnt of gold he can pay and then it might quadruple and all that

    }

}
