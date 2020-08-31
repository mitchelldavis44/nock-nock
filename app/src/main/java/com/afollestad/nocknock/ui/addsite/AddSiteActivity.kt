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

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.content.Intent.CATEGORY_OPENABLE
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import com.afollestad.nocknock.R
import com.afollestad.nocknock.data.model.Site
import com.afollestad.nocknock.data.model.ValidationMode
import com.afollestad.nocknock.ui.DarkModeSwitchActivity
import com.afollestad.nocknock.ui.viewsite.KEY_SITE
import com.afollestad.nocknock.utilities.ext.onTextChanged
import com.afollestad.nocknock.utilities.ext.setTextAndMaintainSelection
import com.afollestad.nocknock.utilities.livedata.distinct
import com.afollestad.nocknock.viewcomponents.ext.dimenFloat
import com.afollestad.nocknock.viewcomponents.ext.isVisibleCondition
import com.afollestad.nocknock.viewcomponents.ext.onScroll
import com.afollestad.nocknock.viewcomponents.livedata.attachLiveData
import com.afollestad.nocknock.viewcomponents.livedata.toViewText
import com.afollestad.nocknock.viewcomponents.livedata.toViewVisibility
import com.afollestad.vvalidator.form
import com.afollestad.vvalidator.form.Form
import kotlinx.android.synthetic.main.activity_addsite.checkIntervalLayout
import kotlinx.android.synthetic.main.activity_addsite.headersLayout
import kotlinx.android.synthetic.main.activity_addsite.inputName
import kotlinx.android.synthetic.main.activity_addsite.inputTags
import kotlinx.android.synthetic.main.activity_addsite.inputUrl
import kotlinx.android.synthetic.main.activity_addsite.loadingProgress
import kotlinx.android.synthetic.main.activity_addsite.responseTimeoutInput
import kotlinx.android.synthetic.main.activity_addsite.responseValidationMode
import kotlinx.android.synthetic.main.activity_addsite.responseValidationSearchTerm
import kotlinx.android.synthetic.main.activity_addsite.retryPolicyLayout
import kotlinx.android.synthetic.main.activity_addsite.scriptInputLayout
import kotlinx.android.synthetic.main.activity_addsite.scrollView
import kotlinx.android.synthetic.main.activity_addsite.sslCertificateBrowse
import kotlinx.android.synthetic.main.activity_addsite.sslCertificateInput
import kotlinx.android.synthetic.main.activity_addsite.textUrlWarning
import kotlinx.android.synthetic.main.activity_addsite.validationModeDescription
import kotlinx.android.synthetic.main.include_app_bar.toolbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlinx.android.synthetic.main.include_app_bar.app_toolbar as appToolbar
import kotlinx.android.synthetic.main.include_app_bar.toolbar_title as toolbarTitle

/** @author Aidan Follestad (@afollestad) */
class AddSiteActivity : DarkModeSwitchActivity() {
  companion object {
    private const val SELECT_CERT_FILE_RQ = 23
  }

  private val viewModel by viewModel<AddSiteViewModel>()
  private lateinit var validationForm: Form

  @SuppressLint("SetTextI18n")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_addsite)
    setupUi()
    setupValidation()

    lifecycle.addObserver(viewModel)

    // Populate view model with initial data
    val model = intent.getSerializableExtra(KEY_SITE) as? Site
    model?.let { viewModel.prePopulateFromModel(model) }

    // Loading
    loadingProgress.observe(this, viewModel.onIsLoading())

    // Name
    inputName.attachLiveData(this, viewModel.name)

    // Tags
    inputTags.attachLiveData(this, viewModel.tags)

    // Url
    inputUrl.attachLiveData(this, viewModel.url)
    viewModel.onUrlWarningVisibility()
        .toViewVisibility(this, textUrlWarning)

    // Timeout
    responseTimeoutInput.attachLiveData(this, viewModel.timeout)

    // Validation mode
    responseValidationMode.attachLiveData(
        lifecycleOwner = this,
        data = viewModel.validationMode,
        outTransformer = { ValidationMode.fromIndex(it) },
        inTransformer = { it.toIndex() }
    )
    viewModel.onValidationModeDescription()
        .toViewText(this, validationModeDescription)

    // Validation search term
    responseValidationSearchTerm.attachLiveData(
        lifecycleOwner = this,
        data = viewModel.validationSearchTerm,
        pullInChanges = false
    )
    viewModel.onValidationSearchTermVisibility()
        .toViewVisibility(this, responseValidationSearchTerm)

    // SSL certificate
    sslCertificateInput.onTextChanged { viewModel.certificateUri.value = it }
    viewModel.certificateUri.distinct()
        .observe(this, Observer { sslCertificateInput.setTextAndMaintainSelection(it) })

    // Headers
    headersLayout.attach(viewModel.headers)
  }

  private fun setupUi() {
    toolbarTitle.setText(R.string.add_site)
    toolbar.run {
      inflateMenu(R.menu.menu_addsite)
      setNavigationIcon(R.drawable.ic_action_close)
      setNavigationOnClickListener { finish() }
    }

    val validationOptionsAdapter = ArrayAdapter(
        this,
        R.layout.list_item_spinner,
        resources.getStringArray(R.array.response_validation_options)
    )
    validationOptionsAdapter.setDropDownViewResource(R.layout.list_item_spinner_dropdown)
    responseValidationMode.adapter = validationOptionsAdapter

    scrollView.onScroll {
      appToolbar.elevation = if (it > appToolbar.measuredHeight / 2) {
        appToolbar.dimenFloat(R.dimen.default_elevation)
      } else {
        0f
      }
    }

    // SSL certificate
    sslCertificateBrowse.setOnClickListener {
      val intent = Intent(ACTION_OPEN_DOCUMENT).apply {
        addCategory(CATEGORY_OPENABLE)
        type = "*/*"
      }
      startActivityForResult(intent, SELECT_CERT_FILE_RQ)
    }
  }

  private fun setupValidation() {
    validationForm = form {
      input(inputName, name = "Name") {
        isNotEmpty().description(R.string.please_enter_name)
      }
      input(inputUrl, name = "URL") {
        isNotEmpty().description(R.string.please_enter_url)
        isUrl().description(R.string.please_enter_valid_url)
      }
      input(responseTimeoutInput, name = "Timeout", optional = true) {
        isNumber().greaterThan(0)
            .description(R.string.please_enter_networkTimeout)
      }
      input(responseValidationSearchTerm, name = "Search term") {
        conditional(responseValidationSearchTerm.isVisibleCondition()) {
          isNotEmpty().description(R.string.please_enter_search_term)
        }
      }
      input(sslCertificateInput, name = "Certificate Path", optional = true) {
        isUri().hasScheme("file", "content")
            .that { it.host != null }
            .description(R.string.please_enter_validCertUri)
      }
      submitWith(toolbar.menu, R.id.commit) {
        viewModel.commit {
          setResult(RESULT_OK)
          finish()
        }
      }
    }

    // Validation script
    scriptInputLayout.attach(
        codeData = viewModel.validationScript,
        visibility = viewModel.onValidationScriptVisibility(),
        form = validationForm
    )

    // Check interval
    checkIntervalLayout.attach(
        valueData = viewModel.checkIntervalValue,
        multiplierData = viewModel.checkIntervalUnit,
        form = validationForm
    )

    // Retry Policy
    retryPolicyLayout.attach(
        timesData = viewModel.retryPolicyTimes,
        minutesData = viewModel.retryPolicyMinutes,
        form = validationForm
    )
  }

  override fun onResume() {
    super.onResume()
    appToolbar.elevation = if (scrollView.scrollY > appToolbar.measuredHeight / 2) {
      appToolbar.dimenFloat(R.dimen.default_elevation)
    } else {
      0f
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    resultData: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, resultData)
    if (requestCode == SELECT_CERT_FILE_RQ && resultCode == RESULT_OK) {
      sslCertificateInput.setText(resultData?.data?.toString() ?: "")
    }
  }
}
