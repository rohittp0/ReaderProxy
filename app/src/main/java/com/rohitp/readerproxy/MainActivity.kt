package com.rohitp.readerproxy

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.rohitp.readerproxy.ui.theme.ReaderProxyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 100)
        } else {
            startVpn()
        }

        enableEdgeToEdge()
        setContent {
            ReaderProxyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(
                        "Reader Mode Proxy is running",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 100 && res == RESULT_OK) startVpn()
        super.onActivityResult(req, res, data)
    }

    private fun startVpn() {
        startService(Intent(this, MyVpnService::class.java))
    }
}