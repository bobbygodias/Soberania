package org.soberania.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import org.soberania.app.packet.lab.TunLabCounters
import org.soberania.app.packet.lab.TunLabPacketSender
import org.soberania.app.vpn.SoberaniaVpnService

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var action: Button
    private lateinit var labTest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        setContentView(buildUi())
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshUi()
    }

    @Deprecated("Used intentionally to keep M0 free of AndroidX dependencies.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            startLabTunnel()
        }
    }

    private fun buildUi(): LinearLayout {
        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
        }

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 34f
            gravity = Gravity.CENTER
        }

        val motto = TextView(this).apply {
            text = getString(R.string.motto)
            textSize = 16f
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
        }

        action = Button(this).apply {
            setOnClickListener {
                if (SoberaniaVpnService.isRunning) {
                    stopService(Intent(this@MainActivity, SoberaniaVpnService::class.java))
                    status.postDelayed({ refreshUi() }, LAB_REFRESH_DELAY_MS)
                } else {
                    requestVpnPermission()
                }
            }
        }

        labTest = Button(this).apply {
            text = getString(R.string.send_lab_packet)
            isEnabled = false

            setOnClickListener {
                isEnabled = false

                Thread({
                    TunLabPacketSender.send()

                    runOnUiThread {
                        labTest.postDelayed({
                            refreshUi()
                        }, LAB_REFRESH_DELAY_MS)
                    }
                }, "Soberania-M0-TestSender").start()
            }
        }

        root.addView(title)
        root.addView(space(20))
        root.addView(motto)
        root.addView(space(48))
        root.addView(status)
        root.addView(space(32))
        root.addView(action)
        root.addView(space(16))
        root.addView(labTest)

        return root
    }

    private fun space(dp: Int): Space {
        val density = resources.displayMetrics.density
        return Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (dp * density).toInt()
            )
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_VPN)
        } else {
            startLabTunnel()
        }
    }

    private fun startLabTunnel() {
        val intent = Intent(this, SoberaniaVpnService::class.java)
            .setAction(SoberaniaVpnService.ACTION_START)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        status.postDelayed({ refreshUi() }, LAB_REFRESH_DELAY_MS)
    }

    private fun refreshUi() {
        if (SoberaniaVpnService.isRunning) {
            val snapshot = TunLabCounters.snapshot()

            status.text = if (snapshot.running) {
                getString(
                    R.string.status_tun_probe,
                    snapshot.ipv4Packets,
                    snapshot.ipv6Packets,
                    snapshot.unknownPackets,
                    snapshot.bytes
                )
            } else {
                getString(R.string.status_tun_probe_unavailable)
            }

            action.setText(R.string.stop_lab)
            labTest.isEnabled = snapshot.running
        } else {
            status.setText(R.string.status_idle)
            action.setText(R.string.activate_lab)
            labTest.isEnabled = false
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val REQUEST_VPN = 1001
        private const val REQUEST_NOTIFICATIONS = 1002
        private const val LAB_REFRESH_DELAY_MS = 250L
    }
}
