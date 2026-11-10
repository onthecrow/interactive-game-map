package com.onthecrow.genshinmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.onthecrow.interactivegamemap.App
import com.onthecrow.interactivegamemap.sync.Synchronizer
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val synchronizer: Synchronizer by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash screen stays until the first frame of the app, and while it isn't known yet whether there is
        // data to show: then the screen of the first load would flash
        installSplashScreen().setKeepOnScreenCondition { synchronizer.state.value.hasLocalData == null }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}