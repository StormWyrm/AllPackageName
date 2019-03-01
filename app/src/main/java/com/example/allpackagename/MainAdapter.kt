package com.example.allpackagename

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder

class MainAdapter(data: List<AppInfo>?) :
    BaseQuickAdapter<AppInfo, BaseViewHolder>(R.layout.item_main, data) {
    override fun convert(helper: BaseViewHolder?, item: AppInfo?) {
        helper?.setText(R.id.tvAppName, item?.appName)
            ?.setText(R.id.tvPackageName, item?.packageName)
            ?.setImageDrawable(R.id.ivAtator, item?.appIcon)
    }
}