package com.tebakdunia

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlin.math.exp
import kotlin.random.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnStreetViewPanoramaReadyCallback {

    private val totalRounds = 5
    private var round = 0
    private var totalScore = 0
    private var target: LatLng? = null
    private var guess: LatLng? = null
    private var roundReady = false
    private var showingResult = false
    private var panoRetries = 0

    private lateinit var map: GoogleMap
    private var pano: StreetViewPanorama? = null
    private var guessMarker: Marker? = null

    private lateinit var hud: TextView
    private lateinit var info: TextView
    private lateinit var btnGuess: Button
    private lateinit var btnToggle: Button
    private lateinit var panel: View

    // Kota-kota dengan cakupan Street View bagus; titik acak dibuat di sekitarnya
    private val seeds = listOf(
        LatLng(-6.2, 106.8), LatLng(35.68, 139.69), LatLng(48.85, 2.35),
        LatLng(40.71, -74.0), LatLng(-33.87, 151.2), LatLng(-23.55, -46.63),
        LatLng(51.5, -0.12), LatLng(52.52, 13.4), LatLng(37.56, 126.97),
        LatLng(19.43, -99.13), LatLng(-33.92, 18.42), LatLng(13.75, 100.5),
        LatLng(41.9, 12.5), LatLng(59.33, 18.06), LatLng(34.05, -118.24),
        LatLng(-34.6, -58.38), LatLng(1.35, 103.82), LatLng(55.75, 37.6)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hud = findViewById(R.id.hud); info = findViewById(R.id.info)
        btnGuess = findViewById(R.id.btnGuess); btnToggle = findViewById(R.id.btnToggle)
        panel = findViewById(R.id.panel)

        (supportFragmentManager.findFragmentById(R.id.streetView)
            as com.google.android.gms.maps.SupportStreetViewPanoramaFragment).getStreetViewPanoramaAsync(this)
        (supportFragmentManager.findFragmentById(R.id.map)
            as SupportMapFragment).getMapAsync(this)

        btnToggle.setOnClickListener { setPanelOpen(panel.layoutParams.height == 0) }
        btnGuess.setOnClickListener { if (showingResult) nextRound() else submitGuess() }
    }

    override fun onMapReady(m: GoogleMap) {
        map = m
        map.uiSettings.isMapToolbarEnabled = false
        map.setOnMapClickListener { p ->
            if (showingResult) return@setOnMapClickListener
            guess = p
            guessMarker?.remove()
            guessMarker = map.addMarker(MarkerOptions().position(p).title("Tebakanmu"))
            btnGuess.isEnabled = true; btnGuess.text = "Tebak!"
        }
        startRoundIfReady()
    }

    override fun onStreetViewPanoramaReady(p: StreetViewPanorama) {
        pano = p
        p.isStreetNamesEnabled = false   // nama jalan disembunyikan supaya tidak mudah
        p.setOnStreetViewPanoramaChangeListener { loc ->
            if (roundReady) return@setOnStreetViewPanoramaChangeListener
            if (loc?.links != null) {          // panorama ditemukan
                roundReady = true
                target = loc.position
            } else if (++panoRetries < 8) {    // tidak ada cakupan -> cari titik lain
                setRandomPosition()
            }
        }
        startRoundIfReady()
    }

    private fun startRoundIfReady() {
        if (pano != null && this::map.isInitialized && round == 0) nextRound()
    }

    private fun nextRound() {
        if (round >= totalRounds) { finishGame(); return }
        round++
        showingResult = false; roundReady = false; panoRetries = 0
        guess = null; guessMarker = null
        map.clear()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(10.0, 10.0), 1f))
        info.visibility = View.GONE
        btnGuess.isEnabled = false; btnGuess.text = "Ketuk peta untuk menebak"
        setPanelOpen(false)
        updateHud()
        setRandomPosition()
    }

    private fun setRandomPosition() {
        val s = seeds.random()
        val p = LatLng(s.latitude + Random.nextDouble(-0.4, 0.4),
                       s.longitude + Random.nextDouble(-0.4, 0.4))
        pano?.setPosition(p, 50_000, StreetViewSource.OUTDOOR)
    }

    private fun submitGuess() {
        val g = guess ?: return
        val t = target ?: return
        val res = FloatArray(1)
        android.location.Location.distanceBetween(g.latitude, g.longitude, t.latitude, t.longitude, res)
        val km = res[0] / 1000.0
        val points = (5000 * exp(-km / 1492.7)).toInt()   // makin dekat makin tinggi, maks 5000
        totalScore += points

        showingResult = true
        map.addMarker(MarkerOptions().position(t).title("Lokasi asli")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        map.addPolyline(PolylineOptions().add(g, t).color(Color.RED).width(6f))
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(
            LatLngBounds.Builder().include(g).include(t).build(), 140))
        info.visibility = View.VISIBLE
        info.text = "Jarak %,.0f km  •  +%d poin".format(km, points)
        btnGuess.text = if (round >= totalRounds) "Lihat hasil akhir" else "Ronde berikutnya"
        setPanelOpen(true)
        updateHud()
    }

    private fun finishGame() {
        AlertDialog.Builder(this)
            .setTitle("Permainan selesai")
            .setMessage("Skor akhir: $totalScore / ${totalRounds * 5000}")
            .setCancelable(false)
            .setPositiveButton("Main lagi") { _, _ ->
                round = 0; totalScore = 0; nextRound()
            }.show()
    }

    private fun setPanelOpen(open: Boolean) {
        val h = resources.displayMetrics.heightPixels
        panel.layoutParams = panel.layoutParams.apply { height = if (open) (h * 0.62).toInt() else 0 }
        btnToggle.visibility = if (open) View.GONE else View.VISIBLE
        if (!open && !showingResult) btnToggle.text = "Buka peta"
    }

    private fun updateHud() { hud.text = "Ronde $round/$totalRounds  •  Skor $totalScore" }
}
