package com.justsoft.nulpschedule.fragments.scheduleaddfragment

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.model.InstituteAndGroup

class InstituteAndGroupArrayAdapter(context: Context) :
    ArrayAdapter<InstituteAndGroup>(context, R.layout.dropdown_menu_popup_item) {

    var mSourceValues: List<InstituteAndGroup> = emptyList()

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        val tuple = getItem(position)!!
        val tv = view as TextView

        if (tuple.group.isNotEmpty())
            tv.text = "${tuple.institute} ${tuple.group}"
        else
            tv.text = tuple.institute

        return view
    }

    fun setAll(values: Collection<InstituteAndGroup>) {
        mSourceValues = mutableListOf(*values.toTypedArray())
        clear()
        addAll(mSourceValues)
    }

    private lateinit var filter: SmartInstituteAndGroupFilter

    override fun getFilter(): Filter {
        if (!this::filter.isInitialized)
            filter = SmartInstituteAndGroupFilter()
        return filter
    }

    inner class SmartInstituteAndGroupFilter : Filter() {

        private fun distinctByInstituteAndSpecialtyPrefix(values: List<InstituteAndGroup>) =
            values.distinctBy { "${it.institute} ${it.group.substringBefore("-")}" }

        private fun distinctByCourseAndGroup(values: List<InstituteAndGroup>) =
            values.distinctBy { it.group.substring(0, it.group.indexOf("-") + 2) }

        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            Log.d("Filter", "Original values size: ${mSourceValues.size}")


            var result = if (constraint == null)
                mSourceValues
            else {
                if (constraint.contains(" ")) {
                    val constraints = constraint.split(' ', limit = 2)
                    mSourceValues.filter {
                        it.institute.contains(constraints[0], true) &&
                                it.group.contains(constraints[1], true)
                    }
                } else {
                    mSourceValues.filter {
                        it.toString().contains(constraint, true)
                    }
                }
            }
            if (result.size > 10) {
                result = distinctByCourseAndGroup(result).map {
                    InstituteAndGroup(
                        it.institute,
                        it.group.substring(0, it.group.indexOf("-") + 2) + "..."
                    )
                }
            }
            if (result.size > 10) {
                result = distinctByInstituteAndSpecialtyPrefix(result).map {
                    InstituteAndGroup(
                        it.institute,
                        it.group.substring(0, it.group.indexOf("-") + 1) + "..."
                    )
                }
            }

            return FilterResults().apply {
                this.values = result
                this.count = result.size
            }
        }


        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            results ?: return
            results.values ?: return
            val list = results.values as List<InstituteAndGroup>
            if (list.isNotEmpty()) {
                synchronized(this) {
                    clear()
                    addAll(list)
                }
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }
}

