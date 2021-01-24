package io.igrant.mobileagent.utils

import android.graphics.Color
import android.text.TextPaint

import android.text.style.ClickableSpan
import android.view.View


open class ReadMoreSpannable(isUnderline: Boolean) : ClickableSpan() {
    private var isUnderline = true

    override fun onClick(p0: View) {
        TODO("Not yet implemented")
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = isUnderline
        ds.color = Color.parseColor("#1b76d3")
    }

    /**
     * Constructor
     */
    init {
        this.isUnderline = isUnderline
    }
}