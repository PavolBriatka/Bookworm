package com.briatka.pavol.bookworm

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
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
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.briatka.pavol.bookworm.clients.BookIsbnReviewClient
import com.briatka.pavol.bookworm.clients.BookTitleReviewClient
import com.briatka.pavol.bookworm.customobjects.Book
import com.briatka.pavol.bookworm.customobjects.BookObject
import com.briatka.pavol.bookworm.models.BookInteractor
import com.briatka.pavol.bookworm.models.BookPresenter
import com.github.ybq.android.spinkit.SpinKitView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import net.cachapa.expandablelayout.ExpandableLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.IOException

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
    private lateinit var viewModel: BookPresenter
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

        viewModel = BookPresenter()

        enter_title_button.setOnClickListener{
            val dialog = MyDialogFragment()
            dialog.show(supportFragmentManager, "MyDialogFragment")
        }

        scan_isbn_button.setOnClickListener {
            dispatchTakePictureIntent()
        }

    }

    override fun onStart() {
        super.onStart()

        initInteractor()

        subscriptions = CompositeDisposable()

        subscriptions.addAll(bookUpdate())
    }


    private fun initInteractor() {

        if (!viewModel.isInteractorReady()){
            viewModel.setInteractor(BookInteractor())
        }


    }

    private fun bookUpdate(): Disposable {
        return viewModel.returnBookData()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { book ->
                    processNetworkResponse(book)
                }
    }

    private fun reviewsFromTitle(title: String) {
        val client = retrofit.create(BookTitleReviewClient::class.java)
        val call = client.getReviews(API_KEY, title)

        call.enqueue(object : Callback<BookObject> {
            override fun onResponse(call: Call<BookObject>, response: Response<BookObject>) {
                //get values
                val bookObject = response.body()?.book
                //process values
                processNetworkResponse(bookObject)
            }

            override fun onFailure(call: Call<BookObject>, t: Throwable) {
                loadingAnimation!!.animate().alpha(0.0f).duration = shortDuration.toLong()
                Toast.makeText(this@MainActivity, "error :(", Toast.LENGTH_SHORT).show()
            }
        })
    }

    
    private fun processNetworkResponse(book: Book?) {
        if (book != null) {
            var reviews = selectSubString(book.reviewsWidget!!)
            reviews = reviews.replace("565", mScreenWidth)
            setupWebView(reviews)
            titleTv!!.text = book.title
            ratingBar!!.rating = java.lang.Float.parseFloat(book.rating!!)
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
                            if (!isbnCode.isNullOrEmpty()) {
                                viewModel.setIsbn(isbnCode!!)
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
