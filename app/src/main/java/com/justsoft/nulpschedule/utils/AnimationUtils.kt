package com.justsoft.nulpschedule.utils

import android.animation.Animator
import android.view.ViewPropertyAnimator

fun ViewPropertyAnimator.animationEnd(block: () -> Unit): ViewPropertyAnimator =
    this.apply {
        setListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                block()
            }

            override fun onAnimationStart(animation: Animator) {
                /* STUB */
            }

            override fun onAnimationCancel(animation: Animator) {
                /* STUB */
            }

            override fun onAnimationRepeat(animation: Animator) {
                /* STUB */
            }
        })
    }
