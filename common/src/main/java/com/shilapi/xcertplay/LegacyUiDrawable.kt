package com.shilapi.xcertplay

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable

internal fun compatibleRipple(colors: ColorStateList, content: Drawable?, mask: Drawable?): Drawable =
    RippleDrawable(colors, content, mask)
