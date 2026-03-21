package vomatix.ru.spring_2026

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.vk.id.VKID
import com.vk.id.onetap.xml.OneTapBottomSheet
import java.security.MessageDigest

class AuthActivity : AppCompatActivity() {

    object VKIDHelper {
        var isInitialized = false
    }

    private enum class ScreenState {
        VK_AUTH,
        CREATE_PIN,
        ENTER_PIN
    }

    private companion object {
        const val PREFS_NAME = "USER"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_NEED_PIN_SETUP = "need_pin_setup"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    private lateinit var prefs: SharedPreferences

    private lateinit var greetingText: TextView
    private lateinit var authButton: ConstraintLayout
    private lateinit var biometricButton: ConstraintLayout
    private lateinit var pinContainer: View
    private lateinit var keyboard: View
    private lateinit var vkidBottomSheet: OneTapBottomSheet

    private lateinit var pinViews: List<View>
    private lateinit var keyViews: List<TextView>

    private var pin = ""
    private var currentState = ScreenState.VK_AUTH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!VKIDHelper.isInitialized) {
            VKID.init(this)
            VKIDHelper.isInitialized = true
        }

        setContentView(R.layout.activity_auth)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        bindViews()
        setupKeypad()
        setupVkAuth()

        // 🔥 ПРОСТАЯ И ПРАВИЛЬНАЯ ЛОГИКА
        val hasPin = !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()

        if (hasPin) {
            // есть PIN → показываем ввод
            showEnterPinScreen()
        } else {
            // нет PIN → показываем VK авторизацию
            showVkAuthScreen()
        }
    }

    private fun bindViews() {
        greetingText = findViewById(R.id.textView11)
        authButton = findViewById(R.id.auth)
        biometricButton = findViewById(R.id.btnBiometric)
        pinContainer = findViewById(R.id.pinContainer)
        keyboard = findViewById(R.id.keyboard)
        vkidBottomSheet = findViewById(R.id.vkid_bottom_sheet)

        pinViews = listOf(
            findViewById(R.id.pin1),
            findViewById(R.id.pin2),
            findViewById(R.id.pin3),
            findViewById(R.id.pin4)
        )

        updatePinUI()
    }

    private fun setupVkAuth() {
        authButton.setOnClickListener {
            vkidBottomSheet.setCallbacks(
                onAuth = { _, _ ->
                    onVkAuthSuccess()
                },
                onFail = { _, fail ->
                    Log.e("AuthActivity", "VK ID error: $fail")
                    Toast.makeText(this, "Ошибка авторизации VK ID", Toast.LENGTH_SHORT).show()
                }
            )
            vkidBottomSheet.show()
        }
    }

    private fun setupKeypad() {
        keyViews = listOf(
            findViewById(R.id.btn1),
            findViewById(R.id.btn2),
            findViewById(R.id.btn3),
            findViewById(R.id.btn4),
            findViewById(R.id.btn5),
            findViewById(R.id.btn6),
            findViewById(R.id.btn7),
            findViewById(R.id.btn8),
            findViewById(R.id.btn9),
            findViewById(R.id.btn0),
            findViewById(R.id.btnDelete)
        )

        keyViews[0].setOnClickListener { addDigit("1");it.pressAnim() }
        keyViews[1].setOnClickListener { addDigit("2");it.pressAnim() }
        keyViews[2].setOnClickListener { addDigit("3");it.pressAnim() }
        keyViews[3].setOnClickListener { addDigit("4");it.pressAnim() }
        keyViews[4].setOnClickListener { addDigit("5");it.pressAnim() }
        keyViews[5].setOnClickListener { addDigit("6");it.pressAnim() }
        keyViews[6].setOnClickListener { addDigit("7");it.pressAnim() }
        keyViews[7].setOnClickListener { addDigit("8");it.pressAnim() }
        keyViews[8].setOnClickListener { addDigit("9");it.pressAnim() }
        keyViews[9].setOnClickListener { addDigit("0");it.pressAnim() }
        keyViews[10].setOnClickListener { deleteDigit();it.pressAnim() }

        biometricButton.setOnClickListener {
            showBiometricLoginPrompt()
        }
    }

    private fun showVkAuthScreen() {
        currentState = ScreenState.VK_AUTH
        greetingText.text = "Добрый вечер,\nавторизуйтесь"

        authButton.visibility = View.VISIBLE
        biometricButton.visibility = View.GONE
        pinContainer.visibility = View.GONE
        keyboard.visibility = View.GONE
        vkidBottomSheet.visibility = View.VISIBLE

        clearPin()
        animateScreen(greetingText, authButton)
    }

    private fun showCreatePinScreen() {
        currentState = ScreenState.CREATE_PIN
        greetingText.text = "Придумайте PIN-код"

        authButton.visibility = View.GONE
        biometricButton.visibility = View.GONE
        pinContainer.visibility = View.VISIBLE
        keyboard.visibility = View.VISIBLE
        keyboard.translationY = 200f
        keyboard.alpha = 0f

        keyboard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .start()
        vkidBottomSheet.visibility = View.GONE

        clearPin()
        animateScreen(greetingText, pinContainer, keyboard)
    }

    private fun showEnterPinScreen() {
        currentState = ScreenState.ENTER_PIN
        greetingText.text = "Введите PIN-код"

        authButton.visibility = View.GONE
        pinContainer.visibility = View.VISIBLE
        keyboard.visibility = View.VISIBLE
        keyboard.translationY = 200f
        keyboard.alpha = 0f

        keyboard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .start()
        vkidBottomSheet.visibility = View.GONE

        clearPin()

        val biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

        if (biometricEnabled && isBiometricAvailable()) {
            biometricButton.visibility = View.VISIBLE

            // 🔥 ВОТ ЭТО ДОБАВЬ
            showBiometricLoginPrompt()
        } else {
            biometricButton.visibility = View.GONE
        }
        animateScreen(greetingText, pinContainer, keyboard, biometricButton)
    }

    private fun onVkAuthSuccess() {
        prefs.edit {
            putBoolean(KEY_NEED_PIN_SETUP, true)
        }
        vkidBottomSheet.visibility = View.GONE
        showCreatePinScreen()
    }

    private fun addDigit(digit: String) {
        if (pin.length >= 4) return

        pin += digit
        updatePinUI()

        if (pin.length == 4) {
            when (currentState) {
                ScreenState.CREATE_PIN -> finishPinSetup()
                ScreenState.ENTER_PIN -> checkEnteredPin()
                ScreenState.VK_AUTH -> Unit
            }
        }
    }

    private fun deleteDigit() {
        if (pin.isEmpty()) return
        pin = pin.dropLast(1)
        updatePinUI()
    }

    private fun updatePinUI() {
        for (i in pinViews.indices) {
            pinViews[i].setBackgroundResource(
                if (i < pin.length) R.drawable.back_green else R.drawable.bg_pin_empty
            )
        }
    }

    private fun clearPin() {
        pin = ""
        updatePinUI()
    }

    private fun finishPinSetup() {
        val enteredPinHash = hashPin(pin)

        prefs.edit {
            putString(KEY_PIN_HASH, enteredPinHash)
            putBoolean(KEY_NEED_PIN_SETUP, false)
        }

        clearPin()
        showBiometricOptInDialog()
    }

    private fun showBiometricOptInDialog() {
        AlertDialog.Builder(this)
            .setTitle("Включить биометрию?")
            .setMessage("Можно будет входить по отпечатку или лицу в следующий раз.")
            .setPositiveButton("Да") { _, _ ->
                if (isBiometricAvailable()) {
                    showBiometricSetupPrompt()
                } else {
                    prefs.edit {
                        putBoolean(KEY_BIOMETRIC_ENABLED, false)
                    }
                    goToMain()
                }
            }
            .setNegativeButton("Нет") { _, _ ->
                prefs.edit {
                    putBoolean(KEY_BIOMETRIC_ENABLED, false)
                }
                goToMain()
            }
            .setCancelable(false)
            .show()
    }

    private fun showBiometricSetupPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Подтвердите биометрию")
            .setSubtitle("Нужно один раз подтвердить вход")
            .setNegativeButtonText("Отмена")
            .setConfirmationRequired(false)
            .build()

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    prefs.edit {
                        putBoolean(KEY_BIOMETRIC_ENABLED, true)
                    }
                    goToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    prefs.edit {
                        putBoolean(KEY_BIOMETRIC_ENABLED, false)
                    }
                    goToMain()
                }
            }
        )

        prompt.authenticate(promptInfo)
    }

    private fun showBiometricLoginPrompt() {
        if (!isBiometricAvailable()) {
            Toast.makeText(this, "Биометрия недоступна", Toast.LENGTH_SHORT).show()
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Вход по биометрии")
            .setSubtitle("Подтвердите вход")
            .setNegativeButtonText("Использовать PIN")
            .setConfirmationRequired(false)
            .build()

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    goToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@AuthActivity, "Вход по биометрии отменён", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@AuthActivity, "Биометрия не распознана", Toast.LENGTH_SHORT).show()
                }
            }
        )

        prompt.authenticate(promptInfo)
    }

    private fun checkEnteredPin() {
        val savedHash = prefs.getString(KEY_PIN_HASH, null)

        if (savedHash != null && savedHash == hashPin(pin)) {
            clearPin()
            goToMain()
        } else {
            Toast.makeText(this, "Неверный PIN-код", Toast.LENGTH_SHORT).show()
            clearPin()
        }
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun hashPin(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.from_left, R.anim.to_left)
        finish()
    }
    fun View.pressAnim() {
        this.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(70)
            .withEndAction {
                this.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                    .start()
            }
            .start()
    }
    private fun animateScreen(vararg views: View) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 80f

            view.postDelayed({
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
                    .start()
            }, (index * 80).toLong())
        }
    }
}