package com.justsoft.nulpschedule.utils

import androidx.recyclerview.widget.DiffUtil

abstract class ListDiffCallback<T>(
    private val oldList: List<T>,
    private val newList: List<T>
): DiffUtil.Callback() {
    override fun getOldListSize(): Int =
        oldList.size

    override fun getNewListSize(): Int =
        newList.size
}