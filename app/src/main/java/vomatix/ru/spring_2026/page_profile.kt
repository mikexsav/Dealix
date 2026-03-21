package vomatix.ru.spring_2026

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.vk.id.VKID

class page_profile : Fragment(R.layout.fragment_page_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val tvRating = view.findViewById<TextView>(R.id.tvRating)
        val rating = DataHolder.rating
        val status = DataHolder.status
        val phoneTv = view.findViewById<TextView>(R.id.phoneNumber)
        val emailTv = view.findViewById<TextView>(R.id.email)
        val statusBlock = view.findViewById<View>(R.id.constraintLayout6)
        val mainCard = view.findViewById<View>(R.id.constraintLayout4)
        val privateBlock = view.findViewById<View>(R.id.privatedata)
        tvStatus.text = status
        tvRating.text = "${rating.toInt()} / 100"

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val avatar = view.findViewById<ImageView>(R.id.profileImage) // 👈 добавь такой id в XML

        // 🔥 получаем данные VK
        val userData = VKID.instance.accessToken?.userData

        if (userData != null) {
            // 📞 телефон
            val rawPhone = userData.phone ?: ""

            if (rawPhone.isNotEmpty()) {
                phoneTv.text = formatPhone(rawPhone)
            } else {
                phoneTv.text = "Не указан"
            }
            val email = userData.email ?: ""
            emailTv.text = if (email.isNotEmpty()) email else "Не указан"


            // имя
            tvName.text = "${userData.lastName} ${userData.firstName}"

            tvRating.animate().alpha(0f).setDuration(0).withEndAction {
                tvRating.animate().alpha(1f).setDuration(400).start()
            }.start()

            // 🔥 ФОТО
            val rawUrl = userData.photo200 ?: ""

            if (rawUrl.isNotEmpty()) {
                val cleanUrl = extractVkAvatarUrl(rawUrl)

                Glide.with(this)
                    .load(cleanUrl)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(avatar)
                avatar.scaleX = 0f
                avatar.scaleY = 0f

                avatar.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                    .start()
            }
        } else {
            tvName.text = "Пользователь"
        }

    }

    // 🔥 фиксим качество фото VK
    private fun extractVkAvatarUrl(originalUrl: String): String {
        val baseUrl = originalUrl.substringBefore("?")
        val query = originalUrl.substringAfter("?", "")

        val updatedParams = query.split("&").map { param ->
            if (param.startsWith("cs=")) {
                "cs=400x400"
            } else {
                param
            }
        }

        return "$baseUrl?${updatedParams.joinToString("&")}"
    }

    private fun formatPhone(phone: String): String {
        // убираем всё кроме цифр
        val digits = phone.filter { it.isDigit() }

        // если номер начинается не с 7 → заменяем
        val clean = when {
            digits.startsWith("8") -> "7" + digits.drop(1)
            digits.startsWith("7") -> digits
            else -> "7$digits"
        }

        return if (clean.length >= 11) {
            "+7 (${clean.substring(1, 4)}) ${clean.substring(4, 7)}-" +
                    "${clean.substring(7, 9)}-${clean.substring(9, 11)}"
        } else {
            phone // fallback
        }
    }



    private fun startAnimations() {
        val v = view ?: return

        val tvName = v.findViewById<TextView>(R.id.tvName)
        val tvStatus = v.findViewById<TextView>(R.id.tvStatus)
        val tvRating = v.findViewById<TextView>(R.id.tvRating)
        val phoneTv = v.findViewById<TextView>(R.id.phoneNumber)
        val emailTv = v.findViewById<TextView>(R.id.email)
        val statusBlock = v.findViewById<View>(R.id.constraintLayout6)
        val privateBlock = v.findViewById<View>(R.id.privatedata)
        val avatar = v.findViewById<ImageView>(R.id.profileImage)

        val elements = listOf(
            tvName,
            tvStatus,
            tvRating,
            phoneTv,
            emailTv,
            statusBlock,
            privateBlock
        )

        // 🔥 сначала скрываем ВСЁ
        elements.forEach {
            it.alpha = 0f
            it.translationY = 120f
        }

        avatar.scaleX = 0f
        avatar.scaleY = 0f

        // 🔥 анимация аватара
        avatar.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
            .start()

        // 🔥 анимация списка
        elements.forEachIndexed { index, view ->
            view.postDelayed({
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()
            }, (index * 100).toLong())
        }
    }

    override fun onResume() {
        super.onResume()

        view?.postDelayed({
            startAnimations()
        }, 100)
    }
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (!hidden) {
            // 🔥 фрагмент стал видимым
            startAnimations()
        }
    }
}