package com.example.projectbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.projectbot.R

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val gradientPaint = Paint()
    private val gradientMatrix = Matrix()

    override fun onDraw(canvas: Canvas) {
        val textShader = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(
                ContextCompat.getColor(context, R.color.gradient_start), // Corrected line
                ContextCompat.getColor(context, R.color.gradient_end)
            ),
            null, Shader.TileMode.CLAMP
        )

        gradientPaint.shader = textShader
        gradientMatrix.setTranslate(0f, 0f)
        textShader.setLocalMatrix(gradientMatrix)

        paint.shader = textShader
        super.onDraw(canvas)
    }
}