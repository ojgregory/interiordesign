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

    override fun returnData(data: Colour) {
        // Put the String to pass back into an Intent and close this activity
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