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

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val avatar = view.findViewById<ImageView>(R.id.profileImage) // 👈 добавь такой id в XML

        // 🔥 получаем данные VK
        val userData = VKID.instance.accessToken?.userData

        if (userData != null) {
            // имя
            tvName.text = "${userData.lastName} ${userData.firstName}"

            // 🔥 ФОТО
            val rawUrl = userData.photo200 ?: ""

            if (rawUrl.isNotEmpty()) {
                val cleanUrl = extractVkAvatarUrl(rawUrl)

                Glide.with(this)
                    .load(cleanUrl)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(avatar)
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
}