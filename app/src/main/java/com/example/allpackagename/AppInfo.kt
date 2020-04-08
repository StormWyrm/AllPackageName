package com.example.allpackagename

import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable

class AppInfo(
    val appName: String = "",
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val appIcon: Drawable?,
    val signature: String
) : Comparable<AppInfo>, Parcelable {
    override fun compareTo(other: AppInfo): Int {
        var num = appName.compareTo(other.appName)
        if (num == 0)
            num = packageName.compareTo(packageName)
        return num
    }

    constructor(source: Parcel) : this(
        source.readString(),
        source.readString(),
        source.readString(),
        source.readLong(),
        null,
        source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(appName)
        writeString(packageName)
        writeString(versionName)
        writeLong(versionCode)
        writeString(signature)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AppInfo> = object : Parcelable.Creator<AppInfo> {
            override fun createFromParcel(source: Parcel): AppInfo = AppInfo(source)
            override fun newArray(size: Int): Array<AppInfo?> = arrayOfNulls(size)
        }
    }
}