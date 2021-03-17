package com.devlomi.commune

import io.reactivex.disposables.CompositeDisposable

interface Base {
    val disposables:CompositeDisposable
}