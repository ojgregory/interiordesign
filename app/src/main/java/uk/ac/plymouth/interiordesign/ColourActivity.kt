package uk.ac.plymouth.interiordesign

import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import uk.ac.plymouth.interiordesign.Fragments.ColourPickerFragment
import uk.ac.plymouth.interiordesign.Fragments.ColoursFragment
import uk.ac.plymouth.interiordesign.R

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ColourActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_colours)
        //val colourPickerFragment = ColourPickerFragment()
        //addFragment(colourPickerFragment)
        val coloursFragment = ColoursFragment()
        addFragment(coloursFragment)
    }

    private fun addFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.colourFragmentContainer, fragment)
        fragmentTransaction.commit()
    }
}