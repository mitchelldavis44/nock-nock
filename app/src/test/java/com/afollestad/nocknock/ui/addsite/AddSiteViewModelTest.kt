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
package com.afollestad.nocknock.ui.addsite

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.afollestad.nocknock.R
import com.afollestad.nocknock.data.model.Header
import com.afollestad.nocknock.data.model.Site
import com.afollestad.nocknock.data.model.SiteSettings
import com.afollestad.nocknock.data.model.Status.WAITING
import com.afollestad.nocknock.data.model.ValidationMode.JAVASCRIPT
import com.afollestad.nocknock.data.model.ValidationMode.STATUS_CODE
import com.afollestad.nocknock.data.model.ValidationMode.TERM_SEARCH
import com.afollestad.nocknock.data.model.ValidationResult
import com.afollestad.nocknock.engine.validation.ValidationExecutor
import com.afollestad.nocknock.mockDatabase
import com.afollestad.nocknock.utilities.ext.MINUTE
import com.afollestad.nocknock.utilities.livedata.test
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** @author Aidan Follestad (@afollestad) */
@ExperimentalCoroutinesApi
class AddSiteViewModelTest {

  private val database = mockDatabase()
  private val validationManager = mock<ValidationExecutor>()

  @Rule @JvmField val rule = InstantTaskExecutorRule()

  private val viewModel = AddSiteViewModel(
      database,
      validationManager,
      Dispatchers.Unconfined,
      Dispatchers.Unconfined
  )

  @After fun tearDown() = viewModel.destroy()

  @Test fun setDefaults() {
    viewModel.setDefaults()

    assertThat(viewModel.name.value).isNull()
    assertThat(viewModel.url.value).isNull()
    assertThat(viewModel.timeout.value).isEqualTo(10000)
    assertThat(viewModel.validationMode.value).isEqualTo(STATUS_CODE)
    assertThat(viewModel.validationSearchTerm.value).isNull()
    assertThat(viewModel.validationScript.value).isNull()
    assertThat(viewModel.checkIntervalValue.value).isEqualTo(0)
    assertThat(viewModel.checkIntervalUnit.value).isEqualTo(MINUTE)
  }

  @Test fun onUrlWarningVisibility() {
    val urlWarningVisibility = viewModel.onUrlWarningVisibility()
        .test()

    viewModel.url.value = ""
    urlWarningVisibility.assertValues(false)

    viewModel.url.value = "helloworld"
    urlWarningVisibility.assertValues(true)

    viewModel.url.value = "http://helloworld.com"
    urlWarningVisibility.assertValues(false)

    viewModel.url.value = "ftp://helloworld.com"
    urlWarningVisibility.assertValues(true)
  }

  @Test fun onValidationModeDescription() {
    val description = viewModel.onValidationModeDescription()
        .test()

    viewModel.validationMode.value = STATUS_CODE
    description.assertValues(R.string.validation_mode_status_desc)

    viewModel.validationMode.value = TERM_SEARCH
    description.assertValues(R.string.validation_mode_term_desc)

    viewModel.validationMode.value = JAVASCRIPT
    description.assertValues(R.string.validation_mode_javascript_desc)
  }

  @Test fun onValidationSearchTermVisibility() {
    val visibility = viewModel.onValidationSearchTermVisibility()
        .test()

    viewModel.validationMode.value = STATUS_CODE
    visibility.assertValues(false)

    viewModel.validationMode.value = TERM_SEARCH
    visibility.assertValues(true)

    viewModel.validationMode.value = JAVASCRIPT
    visibility.assertValues(false)
  }

  @Test fun onValidationScriptVisibility() {
    val visibility = viewModel.onValidationScriptVisibility()
        .test()

    viewModel.validationMode.value = STATUS_CODE
    visibility.assertValues(false)

    viewModel.validationMode.value = TERM_SEARCH
    visibility.assertValues(false)

    viewModel.validationMode.value = JAVASCRIPT
    visibility.assertValues(true)
  }

  @Test fun getCheckIntervalMs() {
    viewModel.checkIntervalValue.value = 3
    viewModel.checkIntervalUnit.value = 200
    assertThat(viewModel.getCheckIntervalMs()).isEqualTo(600L)
  }

  @Test fun getValidationArgs() {
    viewModel.validationSearchTerm.value = "One"
    viewModel.validationScript.value = "Two"

    viewModel.validationMode.value = STATUS_CODE
    assertThat(viewModel.getValidationArgs()).isNull()

    viewModel.validationMode.value = TERM_SEARCH
    assertThat(viewModel.getValidationArgs()).isEqualTo("One")

    viewModel.validationMode.value = JAVASCRIPT
    assertThat(viewModel.getValidationArgs()).isEqualTo("Two")
  }

  @Test fun commit_success() = runBlocking {
    val isLoading = viewModel.onIsLoading()
        .test()

    fillInModel()
    val onDone = mock<() -> Unit>()
    viewModel.commit(onDone)

    val siteCaptor = argumentCaptor<Site>()
    val settingsCaptor = argumentCaptor<SiteSettings>()
    val validationResultCaptor = argumentCaptor<ValidationResult>()

    isLoading.assertValues(true, false)
    verify(database.siteDao()).insert(siteCaptor.capture())
    verify(database.siteSettingsDao()).insert(settingsCaptor.capture())
    verify(database.validationResultsDao()).insert(validationResultCaptor.capture())

    val settings = settingsCaptor.firstValue
    val result = validationResultCaptor.firstValue.copy(siteId = 1)
    val model = siteCaptor.firstValue.copy(
        id = 1, // fill it in because our insert captor doesn't catch this
        settings = settings,
        lastResult = result
    )

    assertThat(result.reason).isNull()
    assertThat(result.status).isEqualTo(WAITING)

    verify(validationManager).scheduleValidation(
        site = model,
        rightNow = true,
        cancelPrevious = true,
        fromFinishingJob = false
    )

    verify(onDone).invoke()
  }

  private fun fillInModel() = viewModel.apply {
    name.value = "Welcome to Wakanda"
    url.value = "https://www.wakanda.gov"
    timeout.value = 10000
    validationMode.value = TERM_SEARCH
    validationSearchTerm.value = "T'Challa"
    validationScript.value = null
    checkIntervalValue.value = 60
    checkIntervalUnit.value = 1000
    tags.value = "one,two"
    headers.value = listOf(
        Header(2L, 1L, key = "Content-Type", value = "text/html"),
        Header(3L, 1L, key = "User-Agent", value = "NockNock")
    )
  }
}
