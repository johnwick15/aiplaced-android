package com.cscaprep.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cscaprep.app.data.ApiClient
import com.cscaprep.app.data.SessionStore
import com.cscaprep.app.ui.CSCAPrepApp
import com.cscaprep.app.ui.CSCAPrepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SessionStore(this)
        val api = ApiClient(session)
        setContent { CSCAPrepTheme { CSCAPrepApp(api, session) } }
    }
}
