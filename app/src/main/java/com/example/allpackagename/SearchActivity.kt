package com.example.allpackagename

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class SearchActivity : AppCompatActivity() {
    private lateinit var mAdapter: MainAdapter

    companion object {
        fun start(context: Context) {
            Intent(context, SearchActivity::class.java).run {
                context.startActivity(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        initToolbar()
    }

    private fun initView() {
        mAdapter = MainAdapter(null).apply {
            setOnItemLongClickListener { _, _, position ->
                getItem(position)?.run {
                    val msg = "应用名：${appName}; 包名：${packageName}; versionCode：${versionCode}; versionName：${versionName}; 签名: ${signature}"
                    val clipboardManager =
                        this@SearchActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.primaryClip = ClipData.newPlainText(null, msg)
                    Toast.makeText(this@SearchActivity, "已复制应用信息到剪切板！", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }
        rv.run {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = mAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val menuItem = menu?.findItem(R.id.search)
        val searchView = MenuItemCompat.getActionView(menuItem) as SearchView
        searchView.run {
            isIconified = false
            queryHint = "请输入应用名/包名"
            setIconifiedByDefault(true)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
                    return true
                }

                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchApp(query)
                    return true
                }
            })
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun searchApp(query: String?) {
        if (query.isNullOrEmpty() || query.trim().isEmpty()) {
            Toast.makeText(this, "搜索词不能为空!", Toast.LENGTH_SHORT).show()
        } else {
            thread {
                mAdapter.emptyView = View.inflate(rv.context, R.layout.layout_loading, null)
                val searchResult = ArrayList<AppInfo>()
                for (appInfo in MainActivity.appInfos) {
                    if (appInfo.appName.contains(query, true) || appInfo.packageName.contains(query, true)) {
                        searchResult.add(appInfo)
                    }
                }
                runOnUiThread {
                    if (searchResult.isEmpty()) {
                        mAdapter.setNewData(null)
                        mAdapter.emptyView = View.inflate(rv.context, R.layout.layout_empty, null)
                    } else {
                        mAdapter.setNewData(searchResult)
                    }
                }
            }.run()
        }
    }

    private fun initToolbar() {
        toolbar.run {
            title = "搜索"
            setSupportActionBar(this)
            setNavigationOnClickListener {
                finish()
            }
        }
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)//设置返回按钮
        }
    }
}