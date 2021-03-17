package com.devlomi.commune.model.realms

import io.realm.RealmObject

open class StatusSeenBy(var user: User?=null, var seenAt: Long = 0):RealmObject(){
    //to use list.contains or list.indexOf

    override fun equals(other: Any?): Boolean {
        if (other is StatusSeenBy) {
            if (this.user?.uid == other.user?.uid)
                return true
        }
        return false
    }




}
