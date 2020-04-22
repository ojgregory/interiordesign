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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import uk.ac.plymouth.interiordesign.Room.Colour
import uk.ac.plymouth.interiordesign.ColourActivity
import uk.ac.plymouth.interiordesign.ColourAdapter
import uk.ac.plymouth.interiordesign.R
import uk.ac.plymouth.interiordesign.Room.ColourDatabase

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val colourDao = ColourDatabase.getDatabase(requireContext()).colourDao()
        GlobalScope.launch {
            if (colourDao.getAll().isEmpty()) {
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
            val colours = colourDao.getAll()
            GlobalScope.launch(Dispatchers.Main) {
                launchAdapter(colours)
            }
        }
    }

    private fun launchAdapter(colours : List<Colour>) {
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