package com.example.allpackagename

import android.graphics.drawable.Drawable

class AppInfo(val appName: String = "", val packageName: String, val appIcon: Drawable?) : Comparable<AppInfo> {
    override fun compareTo(other: AppInfo): Int {
        var num = appName.compareTo(other.appName)
        if (num == 0)
            num = packageName.compareTo(packageName)
        return num
    }
}