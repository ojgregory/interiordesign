package uk.ac.plymouth.interiordesign

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import uk.ac.plymouth.interiordesign.Fragments.ColoursFragment
import uk.ac.plymouth.interiordesign.Fragments.DataReturnInterface
import uk.ac.plymouth.interiordesign.Room.Colour

// Colour Activity
// Sets up the list fragment and returns selected colour to previous activity
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ColourActivity : AppCompatActivity(), DataReturnInterface<Colour> {
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

    // Return colour as individual values
    // Can be recreated at arrival
    override fun returnData(data: Colour) {
        val intent = Intent()
        intent.putExtra("r", data.r)
        intent.putExtra("g", data.g)
        intent.putExtra("b", data.b)
        intent.putExtra("a", data.a)
        intent.putExtra("name", data.name)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

}