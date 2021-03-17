package com.devlomi.commune.adapters.messaging

import androidx.lifecycle.LiveData
import com.devlomi.commune.model.AudibleState

interface AudibleBase {
    var audibleState: LiveData<Map<String, AudibleState>>?
    var audibleInteraction:AudibleInteraction?
}