/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afollestad.nocknock.notifications

import android.annotation.TargetApi
import android.app.NotificationManager
import android.os.Build.VERSION_CODES
import com.afollestad.nocknock.notifications.Channel.ValidationErrors
import com.afollestad.nocknock.utilities.providers.CanNotifyModel
import com.afollestad.nocknock.utilities.providers.IntentProvider
import com.afollestad.nocknock.utilities.providers.NotificationChannelProvider
import com.afollestad.nocknock.utilities.providers.NotificationProvider
import com.afollestad.nocknock.utilities.providers.RealIntentProvider.Companion.BASE_NOTIFICATION_REQUEST_CODE
import com.afollestad.nocknock.utilities.providers.StringProvider
import timber.log.Timber.d as log

/** @author Aidan Follestad (@afollestad) */
interface NockNotificationManager {

  fun setIsAppOpen(open: Boolean)

  fun createChannels()

  fun postValidationErrorNotification(model: CanNotifyModel)

  fun postValidationSuccessNotification(model: CanNotifyModel)

  fun cancelStatusNotification(model: CanNotifyModel)

  fun cancelStatusNotifications()
}

/** @author Aidan Follestad (@afollestad) */
class RealNockNotificationManager(
  private val stockManager: NotificationManager,
  private val stringProvider: StringProvider,
  private val intentProvider: IntentProvider,
  private val channelProvider: NotificationChannelProvider,
  private val notificationProvider: NotificationProvider
) : NockNotificationManager {

  private var isAppOpen = false

  override fun setIsAppOpen(open: Boolean) {
    this.isAppOpen = open
    log("Is app open? $open")
  }

  override fun createChannels() =
    Channel.values().forEach(this::createChannel)

  override fun postValidationErrorNotification(model: CanNotifyModel) {
    if (isAppOpen) {
      // Don't show notifications while the app is open
      log("App is open, validation error notification for site ${model.notifyId()} won't be posted.")
      return
    }

    log("Posting validation error notification for site ${model.notifyId()}...")
    val intent = intentProvider.getPendingIntentForViewSite(model)

    val newNotification = notificationProvider.create(
        channelId = ValidationErrors.id,
        title = model.notifyName(),
        content = model.notifyDescription() ?: stringProvider.get(R.string.something_wrong),
        intent = intent,
        smallIcon = R.drawable.ic_notification_error
    )

    stockManager.notify(model.notifyTag(), model.notificationId(), newNotification)
    log("Posted validation error notification for site ${model.notificationId()}.")
  }

  override fun postValidationSuccessNotification(model: CanNotifyModel) {
    if (isAppOpen) {
      // Don't show notifications while the app is open
      log("App is open, validation success notification for site ${model.notifyId()} won't be posted.")
      return
    }

    log("Posting validation success notification for site ${model.notifyId()}...")
    val intent = intentProvider.getPendingIntentForViewSite(model)

    val newNotification = notificationProvider.create(
        channelId = ValidationErrors.id,
        title = model.notifyName(),
        content = stringProvider.get(R.string.validation_passed),
        intent = intent,
        smallIcon = R.drawable.ic_notification_success
    )

    stockManager.notify(model.notifyTag(), model.notificationId(), newNotification)
    log("Posted validation success notification for site ${model.notificationId()}.")
  }

  override fun cancelStatusNotification(model: CanNotifyModel) {
    stockManager.cancel(model.notificationId())
    log("Cancelled status notification for site ${model.notifyId()}.")
  }

  override fun cancelStatusNotifications() = stockManager.cancelAll()

  @TargetApi(VERSION_CODES.O)
  private fun createChannel(channel: Channel) {
    val notificationChannel = channelProvider.create(
        id = channel.id,
        title = stringProvider.get(channel.title),
        description = stringProvider.get(channel.description),
        importance = channel.importance
    )
    notificationChannel?.let(stockManager::createNotificationChannel)
    log("Created notification channel ${channel.id}")
  }

  private fun CanNotifyModel.notificationId() = BASE_NOTIFICATION_REQUEST_CODE + this.notifyId()
}
