package uk.ac.plymouth.interiordesign.Fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_colour.*
import uk.ac.plymouth.interiordesign.R

class ColourFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_colour, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    fun changeColour(a: Int, r: Int, g: Int, b : Int) {
        //Build and show the new color
        colourTextView.setBackgroundColor(Color.argb(a,r,g,b));
        //show the color value
        colourTextView.text = ("0x"+String.format("%02x", a)+String.format("%02x", r)
                +String.format("%02x", g)+String.format("%02x", b));
        //some math so text shows (needs improvement for greys)
        colourTextView.setTextColor(Color.argb(0xff,255-r,255-g,255-b));
    }
}