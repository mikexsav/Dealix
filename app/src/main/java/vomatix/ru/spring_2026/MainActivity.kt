package vomatix.ru.spring_2026

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val pageMain = PageMain()
    private val pageEffect = page_effect()
    private val pageNews = PageNews()
    private val pageProfile = page_profile()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val api = Connectivity(
            baseUrl = "https://vomatixincorporated.amocrm.ru",
            clientId = "944268f9-68a4-4e56-b761-1e235954ebbb",
            clientSecret = "HSclp4Z1G2c5EAG3skCviu1sscURbpRfjoVMVeqMpTuQN7w8C2JYl3fpqfKNbGp7", // лучше пересоздать
            redirectUri = "https://example.com/oauth"
        )

        api.setTokens(
            access = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImp0aSI6IjBjZTBmOWZiY2JiMjM3MTc5ZmY3MzIwYTUzNTcwOTJiZTQzNzllYjcxYTZmMzk1ZWU4Yjg1MWI0ZTY2MzM2N2Y1MjdiZmRhYzYwODliOWM2In0.eyJhdWQiOiI5NDQyNjhmOS02OGE0LTRlNTYtYjc2MS0xZTIzNTk1NGViYmIiLCJqdGkiOiIwY2UwZjlmYmNiYjIzNzE3OWZmNzMyMGE1MzU3MDkyYmU0Mzc5ZWI3MWE2ZjM5NWVlOGI4NTFiNGU2NjMzNjdmNTI3YmZkYWM2MDg5YjljNiIsImlhdCI6MTc3NDA4MTEzNywibmJmIjoxNzc0MDgxMTM3LCJleHAiOjE3NzQxNjc1MzcsInN1YiI6IjEzNjM0NDc4IiwiZ3JhbnRfdHlwZSI6IiIsImFjY291bnRfaWQiOjMyOTY1NjYyLCJiYXNlX2RvbWFpbiI6ImFtb2NybS5ydSIsInZlcnNpb24iOjIsInNjb3BlcyI6WyJwdXNoX25vdGlmaWNhdGlvbnMiLCJmaWxlcyIsImNybSIsImZpbGVzX2RlbGV0ZSIsIm5vdGlmaWNhdGlvbnMiXSwiaGFzaF91dWlkIjoiNDI2ZjJlNDgtZDM3YS00OWUxLTgyMTktYzg3MTc0OGQ4M2YxIiwiYXBpX2RvbWFpbiI6ImFwaS1iLmFtb2NybS5ydSJ9.ne1-5AlOtSHH3rAPHI603XmPJs5UbMSe0alsJljKtwNWpswtAwbtQIF4NINWSxUZj6AktZ0AxPG4raO2oWUDJCW08_X4w3AD7kFRMsIJE4o52tPNfF9APiaXQ4nSR97DSsuuzZnnAAozWqb_P9cqg5hW0S90BGniCVItA4owKj2XhdpCRxh45tE9CN81sPW2hKXnlF7aruoYaz_lGEyJQTkDCkct7edZz8V1F992lBA9sfHn5o2TwsSjQkdW8twKad2kZ7jQlsZSu2CRqspoAiEiHtPQqc86EZwpAu-s5CZZEqhros5ROCrAm_IaOBz9p1NY37FU3io1XmVoO8_t3g",
            refresh = "def5020023fe93d0ec327f6cf73b2ed1dbcf38531f69f779b2f0286d6b2821d940db86943061dd94477179bf5d681adae8d1f14734e353354b2ca31c2d1b9372f5fc073004ae10bb930d4f7fc979f43a8f5d60ac1dfa6c8c57596689d2a5fd01c76f92e080d1e866dabc4cab631b9ffd72d68723e238ef3d8a0b8db8f2fec43b11e5a8f1c673c5033046a4975790a50996a52e194a04960f908106fa4210c3d63eb0deee540b01844804d85deb5554e02ac10fbec97cbe1bf42cf42482d39b5b5ac85d107d8c81458f146e0cb86aedecd27ae6aacd182480f2755668726d17da1a7960e3d7d2b4cb9cd9992d556c8efe22f53f677fd4f04e2eb3c0f0c97663005ab8ce23f9329497163d8f92988daad3fc5b9044e5cfef8467a251e296f56086812d9aad467c6953b8432f8891e1c224465fa1be7319d9715a1e5d8c5db55ba051c76a5deace8ca7378285ec9cbcf965e52d15eb69d3773b62fec2a65cd43464b66e799336d33094e3a584aaf21b5c29b98baee69ad36f96ad2a0b3dda1849b0c0e2b51cefffff5ed910ce54e1af96512a3df69b4a9643ce6130a8c6ca3bcaf011194fa67bc27c6ccfa6e0b3fc87d7220a8e296b76d076155cc8d2e51d6142d85cf80f333bce4a3acdea61a9f315ba9cf575ab96769580dee565fef0f604700ef74cd669ac8113dd0b6edaf671fc"
        )

        /*lifecycleScope.launch {
            try {
                val res = api.createCarDealVanilla(
                    carName = "BMW X5",
                    saleType = "КРЕДИТ",
                    price = 5500000,
                    managerName = "Иванов Иван Иванович",
                    managerPhone = "+79990000101",
                    managerEmail = "ivanov@mail.ru",
                    companyName = "BMW Центр Москва"
                )
                Log.e("OK", res)
            } catch (e: Exception) {
                Log.e("ERROR", e.message ?: "error")
            }
        }*/
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // добавляем все фрагменты один раз
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
            listOf(pageMain, pageEffect, pageNews, pageProfile).forEach {
                hide(it)
            }
            show(fragment)
            commit()
        }
    }


}