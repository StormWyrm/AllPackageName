package com.example.allpackagename

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import gdut.bsx.share2.FileUtil
import gdut.bsx.share2.Share2
import gdut.bsx.share2.ShareContentType
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.PrintStream
import java.security.MessageDigest
import java.util.*


class MainActivity : AppCompatActivity() {
    private val SYSTEM_APP = 0x00000011
    private val INSTALL_APP = 0x00000022
    private val ALL_APP = 0x00000033

    private var curMode = ALL_APP
    private var adapter: MainAdapter? = null

    private val dialog: AlertDialog by lazy {
        AlertDialog.Builder(this)
            .setMessage("加载中....")
            .setCancelable(false)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadAppinfo()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(
            R.menu.menu_main,
            menu
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.system_app -> {
                if (curMode == SYSTEM_APP) {
                    return true
                }
                curMode = SYSTEM_APP
                loadAppinfo()
            }
            R.id.intall_app -> {
                if (curMode == INSTALL_APP) {
                    return true
                }
                curMode = INSTALL_APP
                loadAppinfo()
            }
            R.id.all_app -> {
                if (curMode == ALL_APP) {
                    return true
                }
                curMode = ALL_APP
                loadAppinfo()
            }
            R.id.share -> {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        val permissions = Array(1) {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }
                        ActivityCompat.requestPermissions(this, permissions, 0)
                    } else {
                        saveTextAndShare()
                    }
                } else {
                    saveTextAndShare()
                }

            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveTextAndShare()
            } else {
                Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTextAndShare() {

        Thread {
            var file = File("$externalCacheDir/packageInfos.txt")
            if (!file.exists()) {
                file.createNewFile()
            }
            val ps = PrintStream(file)
            val appInfos = adapter?.data ?: ArrayList<AppInfo>()
            for (appInfo in appInfos) {
                val msg = "应用名：${appInfo.appName}; 包名：${appInfo.packageName}; 签名: ${appInfo.signature}"
                ps.write(msg.toByteArray())
                ps.println()
            }
            ps.close()

            runOnUiThread {
                Share2.Builder(this)
                    // 指定分享的文件类型
                    .setContentType(ShareContentType.FILE)
                    // 设置要分享的文件 Uri
                    .setShareFileUri(FileUtil.getFileUri(this, ShareContentType.FILE, file))
                    // 设置分享选择器的标题
                    .setTitle("Share Image")
                    .build()
                    // 发起分享
                    .shareBySystem()
            }
        }.start()
    }

    private fun loadAppinfo() {
        dialog.show()
        Thread {
            val appInfos = getAppInfo(curMode)
            Collections.sort(appInfos)
            runOnUiThread {
                dialog.dismiss()
                if (adapter == null) {
                    adapter = MainAdapter(appInfos)
                    adapter?.setOnItemLongClickListener { helper, view, position ->
                        val packageName = (helper.getItem(position) as? AppInfo)?.packageName
                        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.primaryClip = ClipData.newPlainText(null, packageName)
                        Toast.makeText(this, "已复制包名到剪切板！", Toast.LENGTH_SHORT).show()
                        true
                    }
                    rv.layoutManager = LinearLayoutManager(this)
                    rv.adapter = adapter
                } else {
                    adapter?.setNewData(appInfos)
                }
                setAppCount(appInfos.size)
            }
        }.start()
    }

    private fun setAppCount(num: Int) {
        supportActionBar?.title = "${getString(R.string.app_name)}(共${num}个应用)"
    }

    private class MainAdapter(data: List<AppInfo>) :
        BaseQuickAdapter<AppInfo, BaseViewHolder>(R.layout.item_main, data) {
        override fun convert(helper: BaseViewHolder?, item: AppInfo?) {
            helper?.setText(R.id.tvAppName, item?.appName)
                ?.setText(R.id.tvPackageName, item?.packageName)
                ?.setImageDrawable(R.id.ivAtator, item?.appIcon)
        }
    }

    private fun getAppInfo(appType: Int = ALL_APP): List<AppInfo> {
        val packageInfos = packageManager.getInstalledPackages(PackageManager.GET_SIGNATURES)
        val appInfo = ArrayList<AppInfo>()
        for (packageInfo in packageInfos) {
            when (appType) {
                SYSTEM_APP -> {
                    if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                        addToList(packageInfo, appInfo)
                    }
                }
                INSTALL_APP -> {
                    if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                        addToList(packageInfo, appInfo)
                    }
                }
                else -> {
                    addToList(packageInfo, appInfo)
                }
            }

        }
        return appInfo

    }

    private fun addToList(
        packageInfo: PackageInfo,
        appInfo: ArrayList<AppInfo>
    ) {
        val appName = packageInfo.applicationInfo.loadLabel(packageManager)
        val packageName = packageInfo.packageName
        val appIcon = packageInfo.applicationInfo.loadIcon(packageManager)
        val signature = packageInfo.signatures[0].toByteArray()
        appInfo.add(AppInfo(appName as String, packageName, appIcon, getSignatureSha1(signature)))
    }

    private fun getSignatureSha1(bytes: ByteArray) : String {
        val md = MessageDigest.getInstance("SHA1")
        val publicKey = md.digest(bytes)
        val hexString = StringBuilder()
        for (i in publicKey.indices) {
            val appendString = Integer.toHexString(0xFF and publicKey[i].toInt())
                .toUpperCase(Locale.US)
            if (appendString.length == 1)
                hexString.append("0")
            hexString.append(appendString)
            if(i != publicKey.size - 1){
                hexString.append(":")
            }
        }
        return hexString.toString()
    }

}
