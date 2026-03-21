package vomatix.ru.spring_2026

import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment

class support : Fragment(R.layout.fragment_support) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageView9 = view.findViewById<ImageView>(R.id.imageView9)
        val title = view.findViewById<TextView>(R.id.textView)
        val searchBar = view.findViewById<ConstraintLayout>(R.id.search_bar)
        val levelBlock = view.findViewById<ConstraintLayout>(R.id.level_up_constr)
        val ratingBlock = view.findViewById<ConstraintLayout>(R.id.rating_constr)

        // 🔥 список элементов
        val elements = listOf(imageView9, title, levelBlock, ratingBlock, searchBar)

        // 💨 начальное состояние
        elements.forEach {
            it.alpha = 0f
            it.translationY = 80f
        }
        imageView9.setOnClickListener {
            parentFragmentManager.popBackStack()

        }
        // 🚀 анимация появления
        elements.forEachIndexed { index, viewItem ->
            viewItem.postDelayed({
                viewItem.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
            }, (index * 120).toLong())
        }
    }
    fun View.pressAnim() {
        this.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).withEndAction {
            this.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }
}