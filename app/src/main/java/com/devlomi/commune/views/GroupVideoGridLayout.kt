/*
 * Created by Devlomi on 2020
 */




package com.devlomi.commune.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.devlomi.commune.common.extensions.isOdd
import kotlin.collections.ArrayList

class CallingGridLayout @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyle: Int = -1
) : FrameLayout(context, attributeSet, defStyle) {


    private val items = ArrayList<GridItem>()


    fun removeItem(id: Int) {
        items.removeAll { it.id == id }
        updateItems()
        updateViews()
    }

    fun addItem(id: Int, view: View) {
        //prevent duplicates
        if (items.filter { it.id == id }.isNotEmpty()) return

        val index = if (items.isEmpty()) 0 else items.lastIndex + 1
        val gridItem = getGridItem(index, items.size, id, view)
        items.add(gridItem)

        updateItems()
        updateViews()
    }

    private fun updateItems() {
        items.forEachIndexed { index, gridItem ->
            items[index] = getGridItem(index, items.size, gridItem.id, gridItem.view)
        }
    }

    private fun updateViews() {
        removeAllViews()


        val columnsCount = items.maxBy { it.column }?.column ?: 1
        val gridHeight = height / columnsCount
        val gridWidth = width / 2 //2 rows max
        items.forEach {


            it.view?.let { button ->
                addView(button)

                button.layoutParams.height = gridHeight
                button.layoutParams.width = if (it.fullWidth) width else gridWidth

                val viewX = if (it.row == 1) 0f else gridWidth.toFloat()
                button.x = viewX
                val viewY =
                        if (it.column == 1) 0f else (gridHeight.toFloat() * (it.column - 1))
                button.y = viewY

                it.view = button


            }
        }
    }


    private fun getGridItem(index: Int, count: Int, id: Int, view: View? = null): GridItem {
        var fullWidth = false
        var row: Int
        var column: Int

        val previousItemOrDefault = getPreviousItemOrDefault(index)


        if (count == 1) {
            fullWidth = true
            row = 1
            column = 1
        } else if (count == 2) {
            fullWidth = true
            row = 1
            column = index + 1

        } else {

            if (index == 0) {
                if (!count.isOdd()) {
                    fullWidth = true
                }
                row = 1
                column = 1
            } else {

                if (previousItemOrDefault.fullWidth || previousItemOrDefault.row == 2) {

                    //get the next column
                    row = 1
                    column = previousItemOrDefault.column + 1
                } else {
                    row = 2
                    column = previousItemOrDefault.column
                }
            }

        }

        return GridItem(id, view, column, row, fullWidth)
    }

    private fun isFirstItemFullWidth(): Boolean = items.firstOrNull()?.fullWidth ?: false
    private fun getPreviousItemOrDefault(index: Int): GridItem =
            items.getOrNull(index - 1) ?: GridItem(1, null, 1, 1, true)

    fun removeAllItems() {
        items.clear()
        removeAllViews()
    }

}

data class GridItem(
        val id: Int,
        var view: View? = null,
        val column: Int,
        val row: Int,
        val fullWidth: Boolean = false
)

