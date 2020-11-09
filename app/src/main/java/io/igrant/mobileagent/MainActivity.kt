package io.igrant.mobileagent

import android.os.Build
import android.os.Bundle
import android.system.ErrnoException
import android.system.Os
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import org.hyperledger.indy.sdk.LibIndy
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val dataDir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            applicationContext.dataDir
        } else {
            TODO("VERSION.SDK_INT < N")
        }
        System.out.println("datadir=" + dataDir.getAbsolutePath())
        val externalFilesDir: File? = getExternalFilesDir(null)
        val path: String = externalFilesDir!!.absolutePath
        println("axel externalFilesDir=$path")

        try {
            Os.setenv("EXTERNAL_STORAGE", path, true)
        } catch (e: ErrnoException) {
            e.printStackTrace()
        }
        System.loadLibrary("indy")
        LibIndy.init()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}