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
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import gdut.bsx.share2.FileUtil
import gdut.bsx.share2.Share2
import gdut.bsx.share2.ShareContentType
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.PrintStream
import java.security.MessageDigest
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private val SYSTEM_APP = 0x00000011
    private val INSTALL_APP = 0x00000022
    private val ALL_APP = 0x00000033

    private var curMode = ALL_APP
    private var curSortMode = 0 //0:按应用名排序 1：按包名排序
    private lateinit var mAdapter: MainAdapter

    companion object {
        var appInfos: ArrayList<AppInfo> = ArrayList()
    }

    private val dialog: AlertDialog by lazy {
        AlertDialog.Builder(this)
            .setMessage("加载中....")
            .setCancelable(false)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initToolbar()
        initView()
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
            R.id.search -> {
                SearchActivity.start(this)
            }
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
            R.id.sort -> {
                if (item.title.equals("按包名排序")) {
                    appInfos.sortWith(Comparator { o1, o2 ->
                        var num = o1.packageName.compareTo(o2.packageName)
                        if (num == 0)
                            num = o1.appName.compareTo(o1.appName)
                        return@Comparator num
                    })
                    item.title = "按应用名排序"
                    curSortMode = 1
                } else {
                    appInfos.sort()
                    item.title = "按包名排序"
                    curMode = 0
                }
                mAdapter.setNewData(appInfos)
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
            val appInfos = mAdapter?.data ?: ArrayList<AppInfo>()
            for (appInfo in appInfos) {
                val msg = "应用名：${appInfo.appName}; 包名：${appInfo.packageName}; versionCode：${appInfo.versionCode}; versionName：${appInfo.versionName}; 签名: ${appInfo.signature}"
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

    private fun initView() {
        mAdapter = MainAdapter(null).apply {
            setOnItemLongClickListener { _, _, position ->
                getItem(position)?.run {
                    val msg = "应用名：${appName}; 包名：${packageName}; versionCode：${versionCode}; versionName：${versionName}; 签名: ${signature}"
                    val clipboardManager =
                        this@MainActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.primaryClip = ClipData.newPlainText(null, msg)
                    Toast.makeText(this@MainActivity, "已复制应用信息到剪切板！", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }
        rv.run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mAdapter
        }
    }

    private fun initToolbar() {
        toolbar.run {
            title = "AllPackageName"
            setSupportActionBar(this)
        }
    }

    private fun loadAppinfo() {
        dialog.show()
        Thread {
            getAppInfo(curMode)
            if (curSortMode == 0) {
                appInfos.sort()
            } else {
                appInfos.sortWith(Comparator { o1, o2 ->
                    var num = o1.packageName.compareTo(o2.packageName)
                    if (num == 0)
                        num = o1.appName.compareTo(o1.appName)
                    return@Comparator num
                })
            }
            runOnUiThread {
                dialog.dismiss()
                setAppCount(appInfos.size)
                mAdapter.setNewData(appInfos)
            }
        }.start()
    }

    private fun setAppCount(num: Int) {
        val titleStr = "(共${num}个应用)"
        val content = "AllPackageName<small>$titleStr</small>"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            supportActionBar?.title = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY)
        } else {
            supportActionBar?.title = Html.fromHtml(content)
        }
    }

    private fun getAppInfo(appType: Int = ALL_APP) {
        val packageInfos = packageManager.getInstalledPackages(PackageManager.GET_SIGNATURES)
        appInfos.clear()
        for (packageInfo in packageInfos) {
            when (appType) {
                SYSTEM_APP -> {
                    if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                        addToList(packageInfo)
                    }
                }
                INSTALL_APP -> {
                    if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                        addToList(packageInfo)
                    }
                }
                else -> {
                    addToList(packageInfo)
                }
            }

        }

    }

    private fun addToList(packageInfo: PackageInfo) {
        val appName = packageInfo.applicationInfo.loadLabel(packageManager)
        val packageName = packageInfo.packageName
        var versionName = packageInfo.versionName
        if(versionName.isNullOrEmpty())
            versionName = ""
        val versionCode = packageInfo.longVersionCode
        val appIcon = packageInfo.applicationInfo.loadIcon(packageManager)
        val signature = packageInfo.signatures[0].toByteArray()
        appInfos.add(AppInfo(appName as String, packageName, versionName,versionCode,appIcon, getSignatureSha1(signature)))
    }

    private fun getSignatureSha1(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA1")
        val publicKey = md.digest(bytes)
        val hexString = StringBuilder()
        for (i in publicKey.indices) {
            val appendString = Integer.toHexString(0xFF and publicKey[i].toInt())
                .toUpperCase(Locale.US)
            if (appendString.length == 1)
                hexString.append("0")
            hexString.append(appendString)
            if (i != publicKey.size - 1) {
                hexString.append(":")
            }
        }
        return hexString.toString()
    }


}
