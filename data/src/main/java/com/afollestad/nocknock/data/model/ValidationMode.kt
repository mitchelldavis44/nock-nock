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
package com.afollestad.nocknock.data.model

/**
 * Represents the validation mode of a [Site] - this is the type of
 * check that is performed to get the site's current [Status].
 *
 * @author Aidan Follestad (@afollestad)
 */
enum class ValidationMode(val value: Int) {
  /** The site is running normally if its status code is successful. */
  STATUS_CODE(1),
  /** The site is running normally if a piece of text is found in its response body. */
  TERM_SEARCH(2),
  /** The site is running normally if a block of given JavaScript executes successfully. */
  JAVASCRIPT(3);

  fun toIndex() = when (this) {
    STATUS_CODE -> 0
    TERM_SEARCH -> 1
    JAVASCRIPT -> 2
  }

  companion object {

    fun fromValue(value: Int) = when (value) {
      STATUS_CODE.value -> STATUS_CODE
      TERM_SEARCH.value -> TERM_SEARCH
      JAVASCRIPT.value -> JAVASCRIPT
      else -> throw IllegalArgumentException("Unknown validationMode: $value")
    }

    fun fromIndex(index: Int) = when (index) {
      0 -> STATUS_CODE
      1 -> TERM_SEARCH
      2 -> JAVASCRIPT
      else -> throw IllegalArgumentException("Index out of range: $index")
    }
  }
}
