package fr.harmoniamk.statsmkworld.extension

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import fr.harmoniamk.statsmkworld.model.local.WarDetails

fun LineChart.setData(war: WarDetails) {
    var diff = 0
    val data = mutableListOf<Entry>()
    data.add(Entry(0f, 0.0001f))
    war.warTracks.forEachIndexed { index, track ->
        diff += track.track.diffScore
        data.add(Entry((index+1).toFloat(), diff.toFloat()))
    }
    val threshold = 0f
    val entriesAbove = mutableListOf<Entry>()
    val entriesBelow = mutableListOf<Entry>()

   entriesAbove.add(Entry(0f, 0f))

    for (i in 0 until data.size - 1) {
        val x1 = i.toFloat()
        val y1 = data[i]
        val x2 = (i + 1).toFloat()
        val y2 = data[i + 1]
        if ((y1.y - threshold) * (y2.y - threshold) < 0) {
            val xIntersect = x1 + (threshold - y1.y) * (x2 - x1) / (y2.y - y1.y)
            val yIntersect = threshold
            entriesAbove.add(Entry(xIntersect, yIntersect))
            entriesBelow.add(Entry(xIntersect, yIntersect))
        }
        if (y2.y >= 0) entriesAbove.add(Entry(x2, y2.y))
        else entriesBelow.add(Entry(x2, y2.y))
    }

    val dataSetAbove = LineDataSet(entriesAbove, "Above").apply {
        color = Color.GREEN
        setDrawCircles(false)
        lineWidth = 1.5f
        setDrawValues(false)
        setDrawFilled(true)
        fillColor = Color.GREEN
        fillAlpha = 60
    }

    val dataSetBelow = LineDataSet(entriesBelow, "Below").apply {
        color = Color.RED
        setDrawCircles(false)
        lineWidth = 1.5f
        setDrawValues(false)
        setDrawFilled(true)
        fillColor = Color.RED
        fillAlpha = 60
    }
    this.data = LineData(dataSetAbove, dataSetBelow)
    val limitLine = LimitLine(threshold).apply {
        lineColor = Color.BLACK
        lineWidth = 1.5f
    }
    this.axisLeft.addLimitLine(limitLine)
    this.axisLeft.setDrawLimitLinesBehindData(false)
    this.axisRight.isEnabled = false
    this.description.isEnabled = false
    this.legend.isEnabled = false
    this.xAxis.setDrawLabels(false)
    this.moveViewToX(0f)
    this.invalidate()
}