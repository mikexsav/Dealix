package vomatix.ru.spring_2026

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

class MainActivity : AppCompatActivity() {
    private val pageMain = PageMain()
    private val pageEffect = page_effect()
    private val pageNews = PageNews()
    private val pageProfile = page_profile()

    interface SberCalcApi {
        @POST("functions/v1/bright-function")
        suspend fun calculateRating(
            @Header("Authorization") token: String,
            @Header("apikey") apiKey: String,
            @Body request: CalcRequest
        ): Response<CalcResponse>
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://sksjfwnaomhhjtmlwhue.supabase.co/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(SberCalcApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupNavigation()

        runCalculation()
    }

    fun runCalculation() {
        val anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNrc2pmd25hb21oaGp0bWx3aHVlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY0MTg4MDIsImV4cCI6MjA4MTk5NDgwMn0.pAPig3GB4ZEF9UIUgVJeFcV4jWZmra3OXnAeKFpYzAU"

        val requestData = CalcRequest(
            fact_volume = 8.0,
            plan_volume = 10.0,
            fact_deals = 12,
            plan_deals = 10,
            fact_share = 50.0,
            target_share = 50.0,
            approved_apps = 7,
            total_apps = 10
        )

        lifecycleScope.launch {
            try {
                val response = apiService.calculateRating(
                    token = "Bearer $anonKey",
                    apiKey = anonKey,
                    request = requestData
                )

                if (response.isSuccessful) {
                    val data = response.body()
                    val rating = (data?.rating as? Double) ?: 0.0
                    val status = data?.status ?: "Silver"
                    DataHolder.rating = rating
                    DataHolder.status = status
                    // 🔥 ВОТ ЭТО
                    pageMain.updateData(rating, status)
                    Log.d("SBER_DEBUG", "РЕЙТИНГ: ${data?.rating}, СТАТУС: ${data?.status}")
                } else {
                    Log.e("SBER_DEBUG", "Ошибка сервера: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("SBER_DEBUG", "Ошибка сети: ${e.message}")
            }
        }
    }
    fun setBottomNavVisible(visible: Boolean) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (visible) {
            bottomNav.animate()
                .translationY(0f)
                .setDuration(200)
                .start()
        } else {
            bottomNav.animate()
                .translationY(bottomNav.height.toFloat())
                .setDuration(200)
                .start()
        }
    }
    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, pageMain, "MAIN")
            add(R.id.fragment_container, pageEffect, "EFFECT").hide(pageEffect)
            add(R.id.fragment_container, pageNews, "NEWS").hide(pageNews)
            add(R.id.fragment_container, pageProfile, "PROFILE").hide(pageProfile)
            commit()
        }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_main -> showFragment(pageMain)
                R.id.page_effect -> showFragment(pageEffect)
                R.id.page_news -> showFragment(pageNews)
                R.id.page_profile -> showFragment(pageProfile)
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            listOf(pageMain, pageEffect, pageNews, pageProfile).forEach { hide(it) }
            show(fragment)
            commit()
        }
    }
    fun selectBottomItem(itemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = itemId
    }
}