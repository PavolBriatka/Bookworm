package com.briatka.pavol.bookworm

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import butterknife.ButterKnife
import com.briatka.pavol.bookworm.clients.BookTitleReviewClient
import com.briatka.pavol.bookworm.customobjects.Book
import com.briatka.pavol.bookworm.customobjects.BookObject
import com.briatka.pavol.bookworm.models.BookInteractor
import com.briatka.pavol.bookworm.models.BookPresenter
import com.briatka.pavol.bookworm.models.BookViewModel
import com.briatka.pavol.bookworm.retrofit.RetrofitInstance
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), MyDialogFragment.OnInputListener {


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
    private lateinit var vModel: BookViewModel
    private lateinit var subscriptions: CompositeDisposable

    override fun sendInput(input: String) {
        if (review_web_view.alpha > 0) review_web_view.animate().alpha(0.0f).duration = longDuration.toLong()
        if (info_layout.alpha > 0) info_layout.animate().alpha(0.0f).duration = shortDuration.toLong()
        if (expandable_layout.isExpanded) expandable_layout.collapse()
        spinkit_view.animate().alpha(1.0f).duration = shortDuration.toLong()
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
        vModel = ViewModelProviders.of(this).get(BookViewModel::class.java)
        val book = vModel.book
        book.observe(this, Observer<Book> {
            processNetworkResponse(it)
        })

        enter_title_button.setOnClickListener {
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

        //subscriptions.addAll(bookUpdate())
    }


    private fun initInteractor() {

        if (!viewModel.isInteractorReady()) {
            viewModel.setInteractor(BookInteractor())
        }

        if (!vModel.isInteractorReady()) {
            vModel.setInteractor(BookInteractor())
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
        retrofit = RetrofitInstance.retrofitInstance
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
                spinkit_view.animate().alpha(0.0f).duration = shortDuration.toLong()
                Log.d("STATUS", t.message)
                Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun processNetworkResponse(book: Book?) {
        if (book != null) {
            var reviews = selectSubString(book.reviewsWidget!!)
            reviews = reviews.replace("565", mScreenWidth)
            setupWebView(reviews)
            book_title.text = book.title
            _book_rating_bar.rating = java.lang.Float.parseFloat(book.rating!!)
        } else {
            spinkit_view.animate().alpha(0.0f).duration = shortDuration.toLong()
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
            spinkit_view.animate().alpha(1.0f).duration = shortDuration.toLong()
            if (review_web_view.alpha > 0) review_web_view.animate().alpha(0.0f).duration = longDuration.toLong()
            if (info_layout.alpha > 0) info_layout.animate().alpha(0.0f).duration = longDuration.toLong()
            if (expandable_layout.isExpanded) expandable_layout.collapse()
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
                                //viewModel.setIsbn(isbnCode!!)
                                Log.e("QWER9", "1")
                                vModel.setIsbn(isbnCode!!)
                            }
                        }
                    } else {
                        Toast.makeText(this@MainActivity,
                                getString(R.string.wrong_photo_quality_toast),
                                Toast.LENGTH_LONG).show()
                        spinkit_view.animate().alpha(0.0f).duration = shortDuration.toLong()
                    }
                }
                .addOnFailureListener { e ->
                    spinkit_view.animate().alpha(0.0f).duration = shortDuration.toLong()
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
        review_web_view.settings.javaScriptEnabled = true
        review_web_view.loadData(rawData, "text/html", null)
        review_web_view.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                spinkit_view.animate().alpha(0.0f).duration = shortDuration.toLong()
                review_web_view.animate().alpha(1.0f).duration = longDuration.toLong()
                expandable_layout.expand()
                info_layout.animate().alpha(1.0f).duration = longDuration.toLong()
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
                    if (review_web_view.canGoBack()) {
                        review_web_view.goBack()
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
