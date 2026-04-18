package com.pocketsave.core.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * AppWidgetProvider host for [PurchasedItemsGlanceWidget]. The framework
 * instantiates this class via the manifest registration; the Glance runtime
 * takes over from there.
 */
class PurchasedItemsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PurchasedItemsGlanceWidget()
}
