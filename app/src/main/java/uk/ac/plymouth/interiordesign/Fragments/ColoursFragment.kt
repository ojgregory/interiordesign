package uk.ac.plymouth.interiordesign.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_colours.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import uk.ac.plymouth.interiordesign.Room.Colour
import uk.ac.plymouth.interiordesign.ColourActivity
import uk.ac.plymouth.interiordesign.ColourAdapter
import uk.ac.plymouth.interiordesign.R
import uk.ac.plymouth.interiordesign.Room.ColourDatabase

class ColoursFragment : Fragment(), DataReturnInterface<Colour>{
    lateinit var colourReturnInterface: DataReturnInterface<Colour>
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
        val colourDao = ColourDatabase.getDatabase(requireContext()).colourDao()
        var colourAdapter : ColourAdapter
        GlobalScope.launch {
            colourDao.deleteAll()
            colourDao.insert(
                Colour(
                    255,
                    0,
                    0,
                    255,
                    "RED"
                )
            )
            colourDao.insert(
                Colour(
                    0,
                    255,
                    0,
                    255,
                    "GREEN"
                )
            )
            colourDao.insert(
                Colour(
                    0,
                    0,
                    255,
                    255,
                    "BLUE"
                )
            )
            colourDao.insert(
                Colour(
                    255,
                    255,
                    0,
                    255,
                    "YELLOW"
                )
            )
            colourDao.insert(
                Colour(
                    255,
                    0,
                    255,
                    255,
                    "MAGENTA"
                )
            )
            colourDao.insert(
                Colour(
                    0,
                    255,
                    255,
                    255,
                    "CYAN"
                )
            )
            colourDao.insert(
                Colour(
                    255,
                    255,
                    255,
                    255,
                    "WHITE"
                )
            )
            colourDao.insert(
                Colour(
                    0,
                    0,
                    0,
                    255,
                    "BLACK"
                )
            )
        }
        colours.add(
            Colour(
                255,
                0,
                0,
                255,
                "RED"
            )
        )
        colours.add(
            Colour(
                0,
                255,
                0,
                255,
                "GREEN"
            )
        )
        colours.add(
            Colour(
                0,
                0,
                255,
                255,
                "BLUE"
            )
        )
        colours.add(
            Colour(
                255,
                255,
                0,
                255,
                "YELLOW"
            )
        )
        colours.add(
            Colour(
                255,
                0,
                255,
                255,
                "MAGENTA"
            )
        )
        colours.add(
            Colour(
                0,
                255,
                255,
                255,
                "CYAN"
            )
        )
        colours.add(
            Colour(
                255,
                255,
                255,
                255,
                "WHITE"
            )
        )
        colours.add(
            Colour(
                0,
                0,
                0,
                255,
                "BLACK"
            )
        )
            colourAdapter =
                ColourAdapter(requireContext(), R.layout.fragment_colour, colours)

            colourList.adapter = colourAdapter

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        colourReturnInterface = (context as ColourActivity)
    }

    override fun returnData(data: Colour) {
        colourReturnInterface.returnData(data)
    }
}