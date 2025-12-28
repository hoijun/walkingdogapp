package com.tulmunchi.walkingdogapp.presentation.ui.mypage.settingPage

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.tulmunchi.walkingdogapp.databinding.ActivityPrivacyPolicyBinding

class PrivacyWebViewActivity : AppCompatActivity() {
    lateinit var binding: ActivityPrivacyPolicyBinding
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = intent.getStringExtra("uri") ?: ""

        binding.apply {
            privacyPolicyWebView.webViewClient = WebViewClient()
            privacyPolicyWebView.settings.javaScriptEnabled = true
            privacyPolicyWebView.loadUrl(uri)
        }
    }
}