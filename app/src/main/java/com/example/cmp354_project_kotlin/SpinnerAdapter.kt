package com.example.salispos.adapters

import android.text.TextUtils

import gr.escsoft.michaelprimez.searchablespinner.tools.UITools


import android.content.Context
import android.graphics.Color
import android.view.View
import com.amulyakhare.textdrawable.TextDrawable

import com.amulyakhare.textdrawable.util.ColorGenerator


import android.view.ViewGroup
import android.widget.*
import com.example.cmp354_project_kotlin.R
import com.example.cmp354_project_kotlin.Filterable

import gr.escsoft.michaelprimez.searchablespinner.interfaces.ISpinnerSelectedView
import java.util.*
import kotlin.collections.ArrayList
// Original code is taken from : https://github.com/michaelprimez/searchablespinner

class MySpinnerAdapter<T:Filterable>(context: Context, var items:MutableList<T>) :
    ArrayAdapter<T>(context, R.layout.view_list_item,R.id.TxtVw_DisplayName),
    android.widget.Filterable,
    ISpinnerSelectedView {
    private val mContext: Context = context
    private val mBackupList: List<T> = items.toList()

    private val mStringFilter = ItemsFilter()
    override fun getCount(): Int {
        return items.size + 1
    }

    override fun getItem(position: Int): T? {
        return if (position > 0) items[position - 1] else null
    }

    override fun getItemId(position: Int): Long {
        return if (position > 0) items[position-1 ].getId() else -1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view: View? = null
        if (position == 0) {
            view = noSelectionView
        } else {
            view = View.inflate(mContext, R.layout.view_list_item, null)
            val letters: ImageView = view.findViewById(R.id.ImgVw_Letters) as ImageView
            val dispalyName = view.findViewById(R.id.TxtVw_DisplayName) as TextView
            letters.setImageDrawable(getTextDrawable(items[position - 1].toString()))
            dispalyName.text = items[position - 1].toString()
        }
        return view!!
    }

    override fun getSelectedView(position: Int): View {
        var view: View? = null
        if (position == 0) {
            view = noSelectionView
        } else {
            view = View.inflate(mContext, R.layout.view_list_item, null)
            val letters: ImageView = view.findViewById(R.id.ImgVw_Letters) as ImageView
            val dispalyName = view.findViewById(R.id.TxtVw_DisplayName) as TextView
            letters.setImageDrawable(getTextDrawable(items[position - 1].toString()))
            dispalyName.text = items[position - 1].toString()
        }
        return view!!
    }

    override fun getNoSelectionView(): View {
        return View.inflate(mContext, R.layout.view_list_no_selection_item, null)
    }

    private fun getTextDrawable(displayName: String): TextDrawable? {
        var drawable: TextDrawable? = null
        drawable = if (!TextUtils.isEmpty(displayName)) {
            val color2: Int = ColorGenerator.MATERIAL.getColor(displayName)
            TextDrawable.builder()
                .beginConfig()
                .width(UITools.dpToPx(mContext, 32f))
                .height(UITools.dpToPx(mContext, 32f))
                .textColor(Color.WHITE)
                .toUpperCase()
                .endConfig()
                .round()
                .build(displayName.substring(0, 1), color2)
        } else {
            TextDrawable.builder()
                .beginConfig()
                .width(UITools.dpToPx(mContext, 32f))
                .height(UITools.dpToPx(mContext, 32f))
                .endConfig()
                .round()
                .build("?", Color.GRAY)
        }
        return drawable
    }

    override fun getFilter(): Filter {
        return mStringFilter
    }

    inner class ItemsFilter : Filter() {
        protected override fun performFiltering(constraint: CharSequence): FilterResults {
            val filterResults = FilterResults()
            if (TextUtils.isEmpty(constraint)) {
                filterResults.count = mBackupList.size
                filterResults.values = mBackupList
                return filterResults
            }
            val filterStrings: ArrayList<T> = ArrayList()
            for (item in mBackupList) {
                if (item.contains(constraint)) {
                    filterStrings.add(item)
                }
            }
            filterResults.count = filterStrings.size
            filterResults.values = filterStrings
            return filterResults
        }

        protected override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            items = results.values as MutableList<T>
            notifyDataSetChanged()
        }
    }

    private inner class ItemView {
        var mImageView: ImageView? = null
        var mTextView: TextView? = null
    }

    enum class ItemViewType {
        ITEM, NO_SELECTION_ITEM
    }

}