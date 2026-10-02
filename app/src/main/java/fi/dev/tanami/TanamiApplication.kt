package fi.dev.tanami

import android.app.Application
import com.google.firebase.FirebaseApp

class TanamiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            installTanamiAppCheck()
        }
    }
}
