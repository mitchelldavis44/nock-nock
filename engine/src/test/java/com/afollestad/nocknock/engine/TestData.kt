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
package com.afollestad.nocknock.engine

import android.content.Intent
import com.afollestad.nocknock.data.AppDatabase
import com.afollestad.nocknock.data.HeaderDao
import com.afollestad.nocknock.data.RetryPolicyDao
import com.afollestad.nocknock.data.SiteDao
import com.afollestad.nocknock.data.SiteSettingsDao
import com.afollestad.nocknock.data.ValidationResultsDao
import com.afollestad.nocknock.data.model.Header
import com.afollestad.nocknock.data.model.RetryPolicy
import com.afollestad.nocknock.data.model.Site
import com.afollestad.nocknock.data.model.SiteSettings
import com.afollestad.nocknock.data.model.Status
import com.afollestad.nocknock.data.model.Status.OK
import com.afollestad.nocknock.data.model.ValidationMode
import com.afollestad.nocknock.data.model.ValidationMode.STATUS_CODE
import com.afollestad.nocknock.data.model.ValidationResult
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.mock
import java.lang.System.currentTimeMillis

fun fakeIntent(action: String): Intent {
  return mock {
    on { getAction() } doReturn action
  }
}

fun fakeSettingsModel(
  id: Long,
  validationMode: ValidationMode = STATUS_CODE
) = SiteSettings(
    siteId = id,
    validationIntervalMs = 600000,
    validationMode = validationMode,
    validationArgs = null,
    disabled = false,
    networkTimeout = 10000,
    certificate = null
)

fun fakeResultModel(
  id: Long,
  status: Status = OK,
  reason: String? = null
) = ValidationResult(
    siteId = id,
    status = status,
    reason = reason,
    timestampMs = currentTimeMillis()
)

fun fakeRetryPolicy(
  id: Long,
  count: Int = 3,
  minutes: Int = 6
) = RetryPolicy(
    siteId = id,
    count = count,
    minutes = minutes
)

fun fakeHeaders(siteId: Long): List<Header> {
  return listOf(
      Header(id = siteId + 1, siteId = siteId, key = "Content-Type", value = "text/html"),
      Header(id = siteId + 2, siteId = siteId, key = "User-Agent", value = "NockNock")
  )
}

fun fakeModel(id: Long) = Site(
    id = id,
    name = "Test",
    url = "https://test.com",
    tags = "",
    settings = fakeSettingsModel(id),
    lastResult = fakeResultModel(id),
    retryPolicy = fakeRetryPolicy(id),
    headers = fakeHeaders(id)
)

val MOCK_MODEL_1 = fakeModel(1)
val MOCK_MODEL_2 = fakeModel(2)
val MOCK_MODEL_3 = fakeModel(3)
val ALL_MOCK_MODELS = listOf(MOCK_MODEL_1, MOCK_MODEL_2, MOCK_MODEL_3)

fun mockDatabase(): AppDatabase {
  val siteDao = mock<SiteDao> {
    on { insert(isA()) } doReturn 1
    on { one(isA()) } doAnswer { inv ->
      val id = inv.getArgument<Long>(0)
      return@doAnswer when (id) {
        1L -> listOf(MOCK_MODEL_1)
        2L -> listOf(MOCK_MODEL_2)
        3L -> listOf(MOCK_MODEL_3)
        else -> listOf()
      }
    }
    on { all() } doReturn ALL_MOCK_MODELS
    on { update(isA()) } doAnswer { inv ->
      return@doAnswer inv.arguments.size
    }
    on { delete(isA()) } doAnswer { inv ->
      return@doAnswer inv.arguments.size
    }
  }
  val settingsDao = mock<SiteSettingsDao> {
    on { insert(isA()) } doReturn 1L
    on { forSite(isA()) } doAnswer { inv ->
      val id = inv.getArgument<Long>(0)
      return@doAnswer when (id) {
        1L -> listOf(MOCK_MODEL_1.settings!!)
        2L -> listOf(MOCK_MODEL_2.settings!!)
        3L -> listOf(MOCK_MODEL_3.settings!!)
        else -> listOf()
      }
    }
    on { update(isA()) } doReturn 1
    on { delete(isA()) } doReturn 1
  }
  val resultsDao = mock<ValidationResultsDao> {
    on { insert(isA()) } doReturn 1L
    on { forSite(isA()) } doAnswer { inv ->
      val id = inv.getArgument<Long>(0)
      return@doAnswer when (id) {
        1L -> listOf(MOCK_MODEL_1.lastResult!!)
        2L -> listOf(MOCK_MODEL_2.lastResult!!)
        3L -> listOf(MOCK_MODEL_3.lastResult!!)
        else -> listOf()
      }
    }
    on { update(isA()) } doReturn 1
    on { delete(isA()) } doReturn 1
  }
  val retryDao = mock<RetryPolicyDao> {
    on { insert(isA()) } doReturn 1L
    on { forSite(isA()) } doAnswer { inv ->
      val id = inv.getArgument<Long>(0)
      return@doAnswer when (id) {
        1L -> listOf(MOCK_MODEL_1.retryPolicy!!)
        2L -> listOf(MOCK_MODEL_2.retryPolicy!!)
        3L -> listOf(MOCK_MODEL_3.retryPolicy!!)
        else -> listOf()
      }
    }
    on { update(isA()) } doReturn 1
    on { delete(isA()) } doReturn 1
  }
  val headerDao = mock<HeaderDao> {
    on { all() } doReturn MOCK_MODEL_1.headers + MOCK_MODEL_2.headers + MOCK_MODEL_3.headers
    on { forSite(isA()) } doAnswer { inv ->
      val id = inv.getArgument<Long>(0)
      return@doAnswer when (id) {
        1L -> MOCK_MODEL_1.headers
        2L -> MOCK_MODEL_2.headers
        3L -> MOCK_MODEL_3.headers
        else -> listOf()
      }
    }
    on { insert(isA<Header>()) } doReturn 1L
    on { insert(isA<List<Header>>()) } doReturn listOf(1L, 2L)
    on { update(isA()) } doReturn 1
    on { delete(isA()) } doReturn 1
  }

  return mock {
    on { siteDao() } doReturn siteDao
    on { siteSettingsDao() } doReturn settingsDao
    on { validationResultsDao() } doReturn resultsDao
    on { retryPolicyDao() } doReturn retryDao
    on { headerDao() } doReturn headerDao
  }
}
