/*
 * Created by Devlomi on 2020
 */

package com.devlomi.commune.activities.calling.model

enum class CallEndedReason {
    UNKNOWN,
    LOCAL_HUNG_UP,
    LOCAL_REJECTED,
    ERROR,
    REMOTE_HUNG_UP,
    REMOTE_REJECTED,
    NO_ANSWER
}