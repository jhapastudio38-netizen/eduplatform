package app.dreamkorea.smartclass

import android.app.Application
import app.dreamkorea.smartclass.data.AppState

class DreamKoreaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppState.init(this)
    }
}
