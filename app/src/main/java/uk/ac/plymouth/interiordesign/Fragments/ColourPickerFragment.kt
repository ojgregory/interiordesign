package uk.ac.plymouth.interiordesign.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.fragment_colour_picker.*
import uk.ac.plymouth.interiordesign.R
import uk.ac.plymouth.interiordesign.Room.Colour

// Colour creation fragment, uses listeners on seek bar to show
// live preview and uses DataReturnInterface to return colour
class ColourPickerFragment : DialogFragment() {
    lateinit var colourFragment : ColourFragment

    companion object {
        private const val title = "Create a colour"
        fun newInstance(): ColourPickerFragment? {
            val colourPickerFragment = ColourPickerFragment()
            val args = Bundle()
            args.putString("title", title)
            colourPickerFragment.setArguments(args)
            return colourPickerFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_colour_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        colourFragment = ColourFragment()
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.child_fragment_container, colourFragment).commit()

        // Set listeners on SeekBars and on createColour button
        seekBarA.setOnSeekBarChangeListener(seekBarListener)
        seekBarR.setOnSeekBarChangeListener(seekBarListener)
        seekBarG.setOnSeekBarChangeListener(seekBarListener)
        seekBarB.setOnSeekBarChangeListener(seekBarListener)
        createColour.setOnClickListener(createColourListener)
    }

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            var A = seekBarA.progress
            var R = seekBarR.progress
            var G = seekBarG.progress
            var B = seekBarB.progress

            val id = seekBar?.id

            //Applies the new progress value to correct corresponding colour value
            if (id == seekBarA.id)
                A = seekBar.progress
            else if (id == seekBarR.id)
                R = seekBar.progress
            else if (id == seekBarB.id)
                B = seekBar.progress
            else if (id == seekBarG.id)
                G = seekBar.progress

            // Update colour preview
            colourFragment.changeColour(A, R, G, B)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }

    }

    private val createColourListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            // Constructs colour from name and progress
            val name = colourNameInput.text.toString()
            val r = seekBarR.progress
            val g = seekBarG.progress
            val b = seekBarB.progress
            val a = seekBarA.progress

            val colour = Colour(r, g, b, a, name)
            sendColour(colour)
        }

    }

    // Using data  return interface sends colour back to the previous fragment
    fun sendColour(data : Colour) {
        if (targetFragment is DataReturnInterface<*>) {
            val listener = targetFragment as DataReturnInterface<Colour>?
            listener!!.returnData(data)
            dismiss()
        }
    }

}