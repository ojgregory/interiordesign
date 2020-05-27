package uk.ac.plymouth.interiordesign.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_colours.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.ac.plymouth.interiordesign.ColourActivity
import uk.ac.plymouth.interiordesign.Adapters.ColourAdapter
import uk.ac.plymouth.interiordesign.R
import uk.ac.plymouth.interiordesign.Room.Colour
import uk.ac.plymouth.interiordesign.Room.ColourDatabase

// Loads the list of colours from Database and shows using the ArrayAdapter
class ColoursFragment : Fragment(), DataReturnInterface<Colour>{
    lateinit var colourReturnInterface: DataReturnInterface<Colour>
    lateinit var colourAdapter : ColourAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_colours, container, false)
    }

    private val addColourButtonListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            // Create the fragment and show it as a dialog.
            val newFragment = ColourPickerFragment.newInstance()
            newFragment!!.setTargetFragment(this@ColoursFragment, 300);
            newFragment.show(parentFragmentManager, "dialog")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val colourDao = ColourDatabase.getDatabase(requireContext()).colourDao()
        fab.setOnClickListener(addColourButtonListener)
        GlobalScope.launch {
            // Fills data base if  empty
            if (colourDao.getAll().isEmpty()) {
                ColourDatabase.fillColourDB(requireContext())
            }

            // Loads all colours then displays using adapter
            val colours = colourDao.getAll()
            // Returns to main thread for ui interaction
            GlobalScope.launch(Dispatchers.Main) {
                launchAdapter(colours)
            }
        }
    }

    private fun launchAdapter(colours : List<Colour>) {
        colourAdapter =
            ColourAdapter(
                requireContext(),
                R.layout.fragment_colour,
                colours
            )
        colourList.adapter = colourAdapter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        colourReturnInterface = (context as ColourActivity)
    }

    // Adds new colour to list and database
    override fun returnData(data: Colour) {
       GlobalScope.launch {
           val colourDao = ColourDatabase.getDatabase(requireContext()).colourDao()
           colourDao.insert(data)
           GlobalScope.launch(Dispatchers.Main) {
               colourAdapter.add(data)
           }
       }
    }
}