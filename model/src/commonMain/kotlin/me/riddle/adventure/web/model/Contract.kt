/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */
@file:JsExport

package me.riddle.adventure.web.model

import kotlin.js.JsExport

// @formatter:off
const val  ICON_OPEN                                = "▾"
const val  ICON_SHUT                                = "▸"
const val  ICON_LEAF                                = "·"

const val ON_OPEN                                   = "open"
const val ON_MESSAGE                                = "message"
const val ON_CLOSE                                  = "close"
const val ON_ERROR                                  = "error"
const val ON_CLICK                                  = "click"
const val ON_CHANGE                                 = "change"
const val EVENT_DOCUMENT_VISIBILITY_CHANGE          = "visibilitychange"

const val COMMAND_BUILD_DOM                         = "buildStaticDOM"
const val COMMAND_EXPAND_TEAMS                      = "expandTeams"

const val DOM_KEY_STATUS                            = "status"
const val DOM_KEY_RUN                               = "runId"
const val DOM_KEY_BUTTON                            = "button"
// Used in TS
@Suppress("unused")
const val DOM_KEY_CONTAINER                         = "div"
const val DOM_KEY_TREE_ROOT                         = "tree"
const val DOM_KEY_CONNECTION                        = "connection"
const val DOM_KEY_CONNECTION_TEXT                   = "connectionText"
// Used in TS
@Suppress("unused")
const val DOM_KEY_TABLE_ROW                         = "tr"
//const val DOM_KEY_TABLE_DATA                        = "td"

const val DATASET_KEY_SMOKE                         = "smoke"
const val DATASET_KEY_BENCH                         = "bench"
const val DATASET_KEY_LOAD                          = "load"
const val DATASET_KEY_FAIL                          = "boom"

const val DATASET_LEVEL_0                           = "Divisions"
const val DATASET_LEVEL_1                           = "Groups"
const val DATASET_LEVEL_2                           = "Teams"
const val DATASET_LEVEL_3                           = "People"

val UI_FRAMEWORK_PURE_TS                            by lazy { "pure" to "Pure TS" }
val UI_FRAMEWORK_REACT                              by lazy { "react" to "React Core" }
val UI_FRAMEWORK_REACT_APP                          by lazy { "reactApp" to "React Application" }
val UI_FRAMEWORK_KOBWEB                             by lazy { "kobweb" to "KobWeb Composer Core" }
val UI_FRAMEWORK_KOBWEB_APP                         by lazy { "kobwebApp" to "KobWeb Composer Application" }

const val PARAMETER_EVENT_CHANNEL                   = "events"
const val PARAMETER_TYPE                            = "type"
//const val PARAMETER_STATUS                          = DOM_KEY_STATUS
const val PARAMETER_DATASET                         = "datasetKey"
const val PARAMETER_RUN_ID                          = DOM_KEY_RUN
const val PARAMETER_UI_FRAMEWORK                    = "uiFramework"
const val PARAMETER_STEP_SIZE                       = "stepSize"
const val PARAMETER_THREAD_RECOVERY_PAUSE_MS        = "threadRecoveryPauseMs"

const val DATA_KEY_REPORT                           = "report"

/** The attribute [ConnectionStatus.dataState] every stylesheet selects on. */
const val HTML5_DATA_STATE                          = "data-state"

val DEFAULT_UI_FRAMEWORK                            by lazy { UI_FRAMEWORK_PURE_TS.first }
const val DEFAULT_PORT                              = 48080

const val DEFAULT_VALUE_DATASET                     = DATASET_KEY_SMOKE
const val DEFAULT_VALUE_STEP_SIZE                   = 20_000
const val DEFAULT_VALUE_THREAD_RECOVERY_PAUSE_MS    = 37
// @formatter:on

enum class ConnectionStatus(val dataState: String, val label: String, val description: String) {
    CONNECTING("wait", "connecting", "Awaiting connection status"),
    LIVE("live", "live", "Connected and eventing"),
    OFFLINE("stale", "offline", "Disconnected; not communicating"),
    UNKNOWN("unset", "unknown", "Status unknown")
}