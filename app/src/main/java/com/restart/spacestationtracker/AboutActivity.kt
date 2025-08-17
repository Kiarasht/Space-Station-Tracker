package com.restart.spacestationtracker

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.util.Linkify
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.restart.spacestationtracker.databinding.AboutLayoutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: AboutLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val currentNightMode = getResources().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isNightMode
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        binding = AboutLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleWindowInsets()

        binding.appVersion.text = getVersionName()
        initLicenses()
        initVersions()
    }

    private fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.scrollView.updatePadding(bottom = insets.bottom, top = insets.top)

            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initLicenses() {
        val inflater = LayoutInflater.from(this)
        val content = binding.licensesContainer
        val softwareList = resources.getStringArray(R.array.software_list)
        val licenseList = resources.getStringArray(R.array.license_list)

        content.addView(createItemsText(*softwareList))
        softwareList.forEachIndexed { i, software ->
            content.addView(createDivider(inflater, content))
            content.addView(createHeader(software))
            content.addView(createHtmlText(licenseList[i]))
        }
    }

    private fun initVersions() {
        val inflater = LayoutInflater.from(this)
        val content = binding.versionContainer
        val versionTitleList = resources.getStringArray(R.array.version_title_list)
        val versionList = resources.getStringArray(R.array.version_list)

        versionTitleList.forEachIndexed { i, title ->
            content.addView(createDivider(inflater, content))
            content.addView(createHeader(title))
            content.addView(createHtmlText(versionList[i]))
        }
    }

    private fun createHeader(name: String): TextView {
        val s = "<big><b>$name</b></big>"
        return createHtmlText(s)
    }

    private fun createItemsText(vararg names: String): TextView {
        val s = StringBuilder()
        names.forEach { name ->
            if (s.isNotEmpty()) {
                s.append("<br>")
            }
            s.append("- ").append(name)
        }
        return createHtmlText(s.toString())
    }

    private fun createHtmlText(s: String): TextView {
        return TextView(this).apply {
            autoLinkMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES
            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(s)
            }
            val marginPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
            ).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, marginPx, 0, marginPx)
            }
        }
    }

    private fun createDivider(inflater: LayoutInflater, parent: ViewGroup): View {
        return inflater.inflate(R.layout.divider, parent, false)
    }

    private fun getVersionName(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }
}