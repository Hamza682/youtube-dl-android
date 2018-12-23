package com.youtubedl.ui.main.home

import android.arch.lifecycle.Observer
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import com.youtubedl.databinding.FragmentBrowserBinding
import com.youtubedl.di.ActivityScoped
import com.youtubedl.ui.component.adapter.TopPageAdapter
import com.youtubedl.ui.main.base.BaseFragment
import com.youtubedl.util.AppUtil.hideSoftKeyboard
import com.youtubedl.util.AppUtil.showSoftKeyboard
import com.youtubedl.util.ScriptUtil.Companion.FACEBOOK_SCRIPT
import javax.inject.Inject

/**
 * Created by cuongpm on 12/7/18.
 */

@ActivityScoped
class BrowserFragment @Inject constructor() : BaseFragment() {

    companion object {
        fun newInstance() = BrowserFragment()
    }

    private lateinit var browserViewModel: BrowserViewModel

    private lateinit var dataBinding: FragmentBrowserBinding

    private lateinit var topPageAdapter: TopPageAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        browserViewModel = (activity as MainActivity).browserViewModel
        topPageAdapter = TopPageAdapter(ArrayList(0), browserViewModel)

        dataBinding = FragmentBrowserBinding.inflate(inflater, container, false).apply {
            this.viewModel = browserViewModel
            this.webChromeClient = browserWebChromeClient
            this.webViewClient = browserWebViewClient
            this.adapter = topPageAdapter
            this.onKeyListener = onKeyPressEnterListener
        }

        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        browserViewModel.start()
        handleUIEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        browserViewModel.stop()
    }

    private fun onBackPressed() {
        when {
            dataBinding.webview.canGoBack() -> dataBinding.webview.goBack()
            browserViewModel.isShowPage.get() -> browserViewModel.isShowPage.set(false)
            else -> activity?.finish()
        }
    }

    private fun handleUIEvents() {
        browserViewModel.apply {
            changeFocusEvent.observe(activity as MainActivity, Observer { isFocus ->
                isFocus?.let { if (it) showSoftKeyboard(dataBinding.etSearch) else hideSoftKeyboard(dataBinding.etSearch) }
            })
            pressBackBtnEvent.observe(activity as MainActivity, Observer { onBackPressed() })
        }
    }

    private val onKeyPressEnterListener = View.OnKeyListener { v, keyCode, _ ->
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            browserViewModel.loadPage((v as EditText).text.toString())
            return@OnKeyListener true
        }
        return@OnKeyListener false
    }

    private val browserWebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            browserViewModel.progress.set(newProgress)
            super.onProgressChanged(view, newProgress)
        }
    }

    private val browserWebViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            browserViewModel.startPage(view.url)
//            checkLinkStatus(view.url)
//            updateBookmarkMenu(view)
            super.onPageStarted(view, url, favicon)
        }


        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            browserViewModel.textInput.set(url)
            browserViewModel.pageUrl.set(url)
            return super.shouldOverrideUrlLoading(view, url)
        }

        override fun onLoadResource(view: WebView, url: String) {
            browserViewModel.textInput.set(view.url)
//            checkLinkStatus(view.url)
            if (url.contains("facebook.com")) {
                browserViewModel.pageUrl.set(FACEBOOK_SCRIPT)
            }
            super.onLoadResource(view, url)
        }

        override fun onPageFinished(view: WebView, url: String) {
            browserViewModel.finishPage(view.url)
//            checkLinkStatus(view.url)
//            getPresenter().saveWebViewHistory(view)
//            updateBookmarkMenu(view)
            super.onPageFinished(view, url)
        }
    }
}