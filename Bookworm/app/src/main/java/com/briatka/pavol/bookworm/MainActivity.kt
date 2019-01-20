package com.briatka.pavol.bookworm

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast

import com.briatka.pavol.bookworm.clients.BookIsbnReviewClient
import com.briatka.pavol.bookworm.clients.BookTitleReviewClient
import com.briatka.pavol.bookworm.customobjects.Book
import com.briatka.pavol.bookworm.customobjects.BookObject
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer

import net.cachapa.expandablelayout.ExpandableLayout

import java.io.File
import java.io.IOException

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.briatka.pavol.bookworm.models.BookPresenter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class MainActivity : AppCompatActivity(), MyDialogFragment.OnInputListener {


    @BindView(R.id.review_web_view)
    var reviewsWebView: WebView? = null
    @BindView(R.id.title)
    var titleTv: TextView? = null
    @BindView(R.id.rating)
    var ratingTv: TextView? = null
    @BindView(R.id.spinkit_view)
    var loadingAnimation: SpinKitView? = null
    @BindView(R.id.info_layout)
    var infoLayout: LinearLayout? = null
    @BindView(R.id.rating_bar)
    var ratingBar: RatingBar? = null
    @BindView(R.id.main_card_view)
    var mainCardView: CardView? = null
    @BindView(R.id.expandable_layout)
    var expandableLayout: ExpandableLayout? = null

    lateinit var imageBitmap: Bitmap
    lateinit var tempFilePath: String
    lateinit var mScreenWidth: String
    lateinit var mScreenHeight: String
    internal var isbnCode: String? = null
    internal var shortDuration = 500
    internal var longDuration = 1000

    lateinit var image: FirebaseVisionImage
    lateinit var textRecognizer: FirebaseVisionTextRecognizer
    lateinit var detector: FirebaseVisionBarcodeDetector

    lateinit var retrofit: Retrofit

    //NEW BLOCK OF MVVM VARIABLES
    private var viewModel: BookPresenter? = null
    private lateinit var subscriptions: CompositeDisposable

    override fun sendInput(input: String) {
        if (reviewsWebView!!.alpha > 0) reviewsWebView!!.animate().alpha(0.0f).duration = longDuration.toLong()
        if (infoLayout!!.alpha > 0) infoLayout!!.animate().alpha(0.0f).duration = shortDuration.toLong()
        if (expandableLayout!!.isExpanded) expandableLayout!!.collapse()
        loadingAnimation!!.animate().alpha(1.0f).duration = shortDuration.toLong()
        reviewsFromTitle(input)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        getScreenDimens()
        textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        detector = FirebaseVision.getInstance().visionBarcodeDetector

    }

    override fun onStart() {
        super.onStart()

        subscriptions = CompositeDisposable()

        subscriptions.addAll()
    }

    @OnClick(R.id.enter_title_button)
    fun openDialog() {
        val dialog = MyDialogFragment()
        dialog.show(supportFragmentManager, "MyDialogFragment")
    }

    @OnClick(R.id.scan_isbn_button)
    fun scanIsbn() {
        dispatchTakePictureIntent()
    }



    private fun reviewsFromTitle(title: String) {
        val client = retrofit.create(BookTitleReviewClient::class.java)
        val call = client.getReviews(API_KEY, title)

        call.enqueue(object : Callback<BookObject> {
            override fun onResponse(call: Call<BookObject>, response: Response<BookObject>) {
                //get values
                val bookObject = response.body()
                //process values
                processNetworkResponse(bookObject)
            }

            override fun onFailure(call: Call<BookObject>, t: Throwable) {
                loadingAnimation!!.animate().alpha(0.0f).duration = shortDuration.toLong()
                Toast.makeText(this@MainActivity, "error :(", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun reviewsFromIsbn() {
        val client = retrofit.create(BookIsbnReviewClient::class.java)
        val call = client.getReviewsIsbn(isbnCode, API_KEY)

        call.enqueue(object : Callback<BookObject> {
            override fun onResponse(call: Call<BookObject>, response: Response<BookObject>) {
                //get values
                val bookObject = response.body()
                //process values
                processNetworkResponse(bookObject)
            }

            override fun onFailure(call: Call<BookObject>, t: Throwable) {
                loadingAnimation!!.animate().alpha(0.0f).duration = shortDuration.toLong()
                Toast.makeText(this@MainActivity, "error :(", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun processNetworkResponse(bookObject: BookObject?) {
        if (bookObject != null) {
            val book = bookObject.book
            var reviews = selectSubString(book!!.reviewsWidget!!)
            reviews = reviews.replace("565", mScreenWidth)
            setupWebView(reviews)
            titleTv!!.text = book.title
            ratingBar!!.rating = java.lang.Float.parseFloat(book.rating)
        } else {
            loadingAnimation!!.animate().alpha(0.0f).duration = shortDuration.toLong()
            Toast.makeText(this@MainActivity,
                    getString(R.string.isbn_not_found),
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (takePictureIntent.resolveActivity(packageManager) != null) {

            var tempFile: File? = null
            try {
                tempFile = PhotoUtils.mCreateTempFile(this)
            } catch (exception: IOException) {
                exception.printStackTrace()
            }

            if (tempFile != null) {

                tempFilePath = tempFile.absolutePath

                val pictureUri = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY, tempFile)

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            loadingAnimation!!.animate().alpha(1.0f).duration = shortDuration.toLong()
            if (reviewsWebView!!.alpha > 0) reviewsWebView!!.animate().alpha(0.0f).duration = longDuration.toLong()
            if (infoLayout!!.alpha > 0) infoLayout!!.animate().alpha(0.0f).duration = longDuration.toLong()
            if (expandableLayout!!.isExpanded) expandableLayout!!.collapse()
            imageBitmap = PhotoUtils.fixRotation(PhotoUtils.mResampleImage(tempFilePath, this), tempFilePath)
            image = FirebaseVisionImage.fromBitmap(imageBitmap)
            processBarcodeScan()
        }
    }

    private fun processBarcodeScan() {
        detector.detectInImage(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.size > 0) {
                        for (barcode in barcodes) {
                            isbnCode = barcode.rawValue
                            if (!TextUtils.isEmpty(isbnCode)) {
                                reviewsFromIsbn()
                            }
                        }
                    } else {
                        Toast.makeText(this@MainActivity,
                                getString(R.string.wrong_photo_quality_toast),
                                Toast.LENGTH_LONG).show()
                        loadingAnimation!!.animate().alpha(0.0f).duration = shortDuration.toLong()
                    }
                }
                .addOnFailureListener { e ->
                    loadingAnimation!!.animate().alpha(0.0f).duration = shortDuration.toLong()
                    Toast.makeText(this@MainActivity,
                            "Problem with code recognition" + e.toString(),
                            Toast.LENGTH_SHORT).show()
                }
    }

    private fun selectSubString(raw: String): String {
        val firstSplit = raw.split("(?=<iframe)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val secondSplit = firstSplit[1].split("(?<=</iframe>)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return secondSplit[0]
    }

    private fun setupWebView(rawData: String) {
        reviewsWebView!!.settings.javaScriptEnabled = true
        reviewsWebView!!.loadData(rawData, "text/html", null)
        reviewsWebView!!.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                loadingAnimation!!.animate().alpha(0.0f).duration = shortDuration.toLong()
                reviewsWebView!!.animate().alpha(1.0f).duration = longDuration.toLong()
                expandableLayout!!.expand()
                infoLayout!!.animate().alpha(1.0f).duration = longDuration.toLong()
            }
        }
    }

    private fun getScreenDimens() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = this.resources.displayMetrics.density
        val dpWidth = screenWidth / density
        val dpHeight = screenHeight / density
        mScreenWidth = (dpWidth.toInt() - 10).toString()
        mScreenHeight = dpHeight.toInt().toString()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (reviewsWebView!!.canGoBack()) {
                        reviewsWebView!!.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!TextUtils.isEmpty(tempFilePath)) {
            PhotoUtils.deleteFile(this, tempFilePath)
        }
    }

    companion object {

        internal val REQUEST_IMAGE_CAPTURE = 27
        private val API_KEY = "RrXbLty3WjyNPa58H93Rdw"
        private val URL = "https://www.goodreads.com"
        private val FILE_PROVIDER_AUTHORITY = "com.briatka.pavol.bookworm.fileprovider"
    }
}
