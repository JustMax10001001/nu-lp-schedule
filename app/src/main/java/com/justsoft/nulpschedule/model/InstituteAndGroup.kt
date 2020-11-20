package com.justsoft.nulpschedule.model

data class InstituteAndGroup(val institute: String, val group: String) {
    override fun toString(): String {
        return "$institute $group"
    }
}