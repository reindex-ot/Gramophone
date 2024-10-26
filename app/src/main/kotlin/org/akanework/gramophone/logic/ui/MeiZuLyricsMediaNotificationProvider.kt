package org.akanework.gramophone.logic.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.isManualNotificationUpdate
import androidx.media3.session.doUpdateNotification
import com.google.common.collect.ImmutableList
import org.akanework.gramophone.R

private const val FLAG_ALWAYS_SHOW_TICKER = 0x01000000
private const val FLAG_ONLY_UPDATE_TICKER = 0x02000000

@OptIn(UnstableApi::class)
private class InnerMeiZuLyricsMediaNotificationProvider(context: Context)
	: DefaultMediaNotificationProvider(context) {
	var ticker: CharSequence? = null
	override fun addNotificationActions(
		mediaSession: MediaSession,
		mediaButtons: ImmutableList<CommandButton>,
		builder: NotificationCompat.Builder,
		actionFactory: MediaNotification.ActionFactory
	): IntArray {
		builder.setTicker(ticker)
		return super.addNotificationActions(mediaSession, mediaButtons, builder, actionFactory)
	}
}

@OptIn(UnstableApi::class)
class MeiZuLyricsMediaNotificationProvider(context: MediaSessionService,
                                           private val tickerProvider: () -> CharSequence?)
	: MediaNotification.Provider {
	private val inner = InnerMeiZuLyricsMediaNotificationProvider(context).apply {
		setSmallIcon(R.drawable.ic_gramophone_monochrome)
	}

	override fun createNotification(
		mediaSession: MediaSession,
		customLayout: ImmutableList<CommandButton>,
		actionFactory: MediaNotification.ActionFactory,
		onNotificationChangedCallback: MediaNotification.Provider.Callback
	): MediaNotification {
		val ticker = tickerProvider()
		inner.ticker = ticker
		return inner.createNotification(
			mediaSession, customLayout, actionFactory
		) {
			onNotificationChangedCallback.onNotificationChanged(it.also {
				if (ticker != null)
					it.applyNotificationFlags(false)
			})
		}.also {
			if (ticker != null)
				it.applyNotificationFlags(isManualNotificationUpdate)
		}
	}

	override fun handleCustomCommand(
		session: MediaSession,
		action: String,
		extras: Bundle
	) = inner.handleCustomCommand(session, action, extras)

	private fun MediaNotification.applyNotificationFlags(onlyUpdateTicker: Boolean) {
		notification.apply {
			extras.putInt("ticker_icon", R.drawable.ic_gramophone_mono16)
			// set to true if icon changed and SysUI has to dispose of cached one
			extras.putBoolean("ticker_icon_switch", false)
			// Keep the status bar lyrics scrolling
			flags = flags.or(FLAG_ALWAYS_SHOW_TICKER)
			// Only update the ticker (lyrics), and do not update other properties
			if (onlyUpdateTicker)
				flags = flags.or(FLAG_ONLY_UPDATE_TICKER)
		}
	}

}