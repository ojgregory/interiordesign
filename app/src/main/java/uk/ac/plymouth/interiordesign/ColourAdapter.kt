package uk.ac.plymouth.interiordesign

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.fragment_colour.view.*
import uk.ac.plymouth.interiordesign.Fragments.DataReturnInterface
import uk.ac.plymouth.interiordesign.Room.Colour


class ColourAdapter(context: Context, resource: Int, objects: List<Colour>) :
    ArrayAdapter<Colour>(context, resource, objects) {
    var colourReturnInterface: DataReturnInterface<Colour> = (context as ColourActivity)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val colour = getItem(position)
        val newView : View = convertView ?: LayoutInflater.from(context).inflate(R.layout.fragment_colour, parent, false)

        changeColour(colour, newView)
        newView.colourTextView.setOnClickListener {
            colourReturnInterface.returnData(getItem(position)!!)
        }

        // Return the completed view to render on screen
        return newView
    }

    private fun changeColour(colour: Colour?, view: View) {
        //Build and show the new color
        view.colourTextView.setBackgroundColor(Color.argb(colour!!.a,colour.r,colour.g,colour.b));
        //show the color value
        view.colourTextView.text = colour.name
        //some math so text shows (needs improvement for greys)
        view.colourTextView.setTextColor(Color.argb(0xff,255-colour.r,255-colour.g,255-colour.b));
    }
}