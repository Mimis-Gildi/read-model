/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

package me.riddle.adventure.web.model

// @formatter:off
const val EVENT_DOCUMENT_VISIBILITY_CHANGE          = "visibilitychange"

const val COMMAND_BUILD_DOM                         = "buildStaticDOM"

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
const val PARAMETER_DATASET                         = "datasetKey"
const val PARAMETER_RUN_ID                          = "runId"
const val PARAMETER_UI_FRAMEWORK                    = "uiFramework"
const val PARAMETER_STEP_SIZE                       = "stepSize"
const val PARAMETER_THREAD_RECOVERY_PAUSE_MS        = "threadRecoveryPauseMs"


val DEFAULT_UI_FRAMEWORK                            by lazy { UI_FRAMEWORK_PURE_TS.first }
const val DEFAULT_PORT                              = 48080

const val DEFAULT_VALUE_DATASET                     = DATASET_KEY_SMOKE
const val DEFAULT_VALUE_STEP_SIZE                   = 20_000
const val DEFAULT_VALUE_THREAD_RECOVERY_PAUSE_MS    = 37L
// @formatter:on
