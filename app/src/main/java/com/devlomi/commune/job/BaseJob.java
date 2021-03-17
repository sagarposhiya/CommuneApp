package com.devlomi.commune.job;

import android.app.job.JobService;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.devlomi.commune.utils.network.FireManager;

import io.reactivex.disposables.CompositeDisposable;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
abstract public class BaseJob extends JobService {
    FireManager fireManager = new FireManager();
    CompositeDisposable disposables = new CompositeDisposable();

}
