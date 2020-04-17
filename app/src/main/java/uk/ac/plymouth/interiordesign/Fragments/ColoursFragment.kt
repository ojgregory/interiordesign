package uk.ac.plymouth.interiordesign.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_colours.*
import uk.ac.plymouth.interiordesign.Colour
import uk.ac.plymouth.interiordesign.ColourAdapter
import uk.ac.plymouth.interiordesign.R

class ColoursFragment : Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_colours, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val colours = mutableListOf<Colour>()
        colours.add(Colour(255, 0, 0, 255, "RED"))
        colours.add(Colour(0, 255, 0, 255, "GREEN"))
        colours.add(Colour(0, 0, 255, 255, "BLUE"))
        colours.add(Colour(255, 255, 0, 255, "YELLOW"))
        colours.add(Colour(255, 0, 255, 255, "MAGENTA"))
        colours.add(Colour(0, 255, 255, 255, "CYAN"))
        colours.add(Colour(255, 255, 255, 255, "WHITE"))
        colours.add(Colour(0, 0, 0, 255, "BLACK"))
        val colourAdapter = ColourAdapter(this.context!!, R.layout.fragment_colour, colours)
        colourList.adapter = colourAdapter
    }
}