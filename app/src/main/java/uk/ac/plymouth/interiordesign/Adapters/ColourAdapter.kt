package uk.ac.plymouth.interiordesign.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.fragment_colour.view.*
import uk.ac.plymouth.interiordesign.ColourActivity
import uk.ac.plymouth.interiordesign.Fragments.DataReturnInterface
import uk.ac.plymouth.interiordesign.R
import uk.ac.plymouth.interiordesign.Room.Colour

// Applies colours from list to fragment_colour layouts
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

    // Change colour applies name, colour and changes text colour
    // to be visible
    private fun changeColour(colour: Colour?, view: View) {
        //Show the color needs to be in specific format
        view.colourTextView.setBackgroundColor(Color.argb(colour!!.a,colour.r,colour.g,colour.b));
        //show the color name
        view.colourTextView.text = colour.name
        // Ensure text is visible
        view.colourTextView.setTextColor(Color.argb(0xff,255-colour.r,255-colour.g,255-colour.b));
    }
}