package uk.ac.plymouth.interiordesign.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.fragment_colour_picker.*
import uk.ac.plymouth.interiordesign.R


class ColourPickerFragment : Fragment() {
    lateinit var colourFragment : ColourFragment
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
        seekBarA.setOnSeekBarChangeListener(seekBarListener)
        seekBarR.setOnSeekBarChangeListener(seekBarListener)
        seekBarG.setOnSeekBarChangeListener(seekBarListener)
        seekBarB.setOnSeekBarChangeListener(seekBarListener)
    }

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            var A = seekBarA.progress
            var R = seekBarR.progress
            var G = seekBarG.progress
            var B = seekBarB.progress

            val id = seekBar?.id

            if (id == seekBarA.id)
                A = seekBar.progress
            else if (id == seekBarR.id)
                R = seekBar.progress
            else if (id == seekBarB.id)
                B = seekBar.progress
            else if (id == seekBarG.id)
                G = seekBar.progress

            colourFragment.changeColour(A, R, G, B)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }

    }

}