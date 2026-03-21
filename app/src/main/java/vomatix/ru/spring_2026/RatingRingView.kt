package vomatix.ru.spring_2026

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class RatingRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var volume = 0f
    private var deals = 0f
    private var share = 0f

    fun setData(volume: Float, deals: Float, share: Float) {
        this.volume = volume
        this.deals = deals
        this.share = share
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val total = volume + deals + share
        if (total == 0f) return

        val rect = RectF(50f, 50f, width - 50f, height - 50f)

        var startAngle = -90f

        // 🔵 Volume
        paint.color = Color.parseColor("#00BCD4")
        val angle1 = 360f * (volume / total)
        canvas.drawArc(rect, startAngle, angle1, false, getStrokePaint())
        startAngle += angle1

        // 🟡 Deals
        paint.color = Color.parseColor("#FFC107")
        val angle2 = 360f * (deals / total)
        canvas.drawArc(rect, startAngle, angle2, false, getStrokePaint())
        startAngle += angle2

        // 🟣 Share
        paint.color = Color.parseColor("#FF00FF")
        val angle3 = 360f * (share / total)
        canvas.drawArc(rect, startAngle, angle3, false, getStrokePaint())
    }

    private fun getStrokePaint(): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 40f
            strokeCap = Paint.Cap.ROUND
            color = paint.color
        }
    }
}