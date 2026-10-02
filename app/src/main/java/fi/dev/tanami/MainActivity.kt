package fi.dev.tanami

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fi.dev.tanami.ui.TanamiApp
import fi.dev.tanami.ui.TanamiTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TanamiTheme { TanamiApp() } }
    }
}
