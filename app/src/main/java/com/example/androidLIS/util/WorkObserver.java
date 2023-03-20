/*
 * Copyright (C) Apulsetech,co.ltd
 * Apulsetech, Shenzhen, China
 *
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice is
 * included in all copies of any software which is or includes a copy or
 * modification of this software and in all copies of the supporting
 * documentation for such software.
 *
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY. IN PARTICULAR, NEITHER THE AUTHOR NOR APULSETECH MAKES ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY OF
 * THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 *
 *
 * Project: ⍺X11 SDK
 *
 * File: HexTextWatcher.java
 * Date: 2020.09.29
 * Author: Tony Park, tonypark@apulsetech.com
 *
 ****************************************************************************
 */

package com.example.androidLIS.util;

import android.app.ProgressDialog;
import android.content.Context;

import java.util.concurrent.TimeUnit;

import rx.android.schedulers.AndroidSchedulers;

public class WorkObserver {

    @SuppressWarnings("unused")
    private static final String TAG = "WorkObserver";
    @SuppressWarnings("unused")
    private static final boolean D = false;

    public static void waitFor(Context context, String message, final ObservableWork work) {
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(message);
        dialog.show();
        rx.Observable.timer(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        v-> {
                            work.onWorkDone(work.run());
                            dialog.cancel();
                        });
    }

    public interface ObservableWork {
        Object run();
        void onWorkDone(Object result);
    }
}
