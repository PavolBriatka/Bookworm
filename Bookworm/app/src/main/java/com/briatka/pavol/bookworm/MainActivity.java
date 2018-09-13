package com.briatka.pavol.bookworm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.briatka.pavol.bookworm.clients.BookIsbnReviewClient;
import com.briatka.pavol.bookworm.clients.BookTitleReviewClient;
import com.briatka.pavol.bookworm.customobjects.Book;
import com.briatka.pavol.bookworm.customobjects.BookObject;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class MainActivity extends AppCompatActivity implements MyDialogFragment.OnInputListener {



    @BindView(R.id.scan_isbn_button)
    Button scanIsbnButton;
    @BindView(R.id.enter_title_button)
    Button openDialogButton;
    @BindView(R.id.review_web_view)
    WebView reviewsWebView;
    @BindView(R.id.title)
    TextView titleTv;
    @BindView(R.id.rating)
    TextView ratingTv;
    @BindView(R.id.spinkit_view)
    SpinKitView loadingAnimation;
    @BindView(R.id.center_image)
    ImageView centerImage;

    static final int REQUEST_IMAGE_CAPTURE = 27;
    private static final String API_KEY = "RrXbLty3WjyNPa58H93Rdw";
    private static final String URL = "https://www.goodreads.com";
    private static final String FILE_PROVIDER_AUTHORITY = "com.briatka.pavol.bookworm.fileprovider";

    Bitmap imageBitmap;
    String tempFilePath;
    String screenWidth;
    String isbnCode;
    int duration = 500;

    FirebaseVisionImage image;
    FirebaseVisionTextRecognizer textRecognizer;
    FirebaseVisionBarcodeDetector detector;

    Retrofit retrofit;

    @Override
    public void sendInput(String input) {
        reviewsWebView.loadUrl("about:blank");
        reviewsWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                loadingAnimation.animate().alpha(1.0f).setDuration(duration);
            }
        });
        loadingAnimation.animate().alpha(1.0f).setDuration(duration);
        reviewsFromTitle(input);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        screenWidth = getScreenWidth();
        textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector();

        /*AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(2000);
        fadeOut.setRepeatCount(Animation.INFINITE);
        //fadeOut.setInterpolator(new DecelerateInterpolator());

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(2000);
        fadeIn.setStartOffset(2000 + fadeOut.getStartOffset());
        fadeIn.setRepeatCount(Animation.INFINITE);
        //fadeIn.setInterpolator(new DecelerateInterpolator());

        AnimationSet animSet = new AnimationSet(false);
        animSet.addAnimation(fadeOut);
        animSet.addAnimation(fadeIn);
        centerImage.setAnimation(animSet);*/


        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(URL)
                .client(new OkHttpClient())
                .addConverterFactory(SimpleXmlConverterFactory.create());

        retrofit = builder.build();
    }


    @OnClick(R.id.enter_title_button)
    void openDialog() {
        MyDialogFragment dialog = new MyDialogFragment();
        dialog.show(getSupportFragmentManager(), "MyDialogFragment");
    }

    @OnClick(R.id.scan_isbn_button)
    void scanIsbn() {
        dispatchTakePictureIntent();
    }


    private void reviewsFromTitle(String title) {
        BookTitleReviewClient client = retrofit.create(BookTitleReviewClient.class);
        Call<BookObject> call =client.getReviews(API_KEY,title);

        call.enqueue(new Callback<BookObject>() {
            @Override
            public void onResponse(Call<BookObject> call, Response<BookObject> response) {
                //get values
                BookObject bookObject = response.body();
                if(bookObject != null) {
                    Book book = bookObject.getBook();
                    String title = String.format(getResources().getString(R.string.title_string), book.getTitle());
                    String rating = String.format(getResources().getString(R.string.rating_string), book.getRating());
                    String reviews = selectSubString(book.getReviewsWidget());
                    //process values
                    reviews = reviews.replace("565", screenWidth);
                    setupWebView(reviews);
                    titleTv.setText(title);
                    ratingTv.setText(rating);
                } else {
                    loadingAnimation.animate().alpha(0.0f).setDuration(duration);
                    Toast.makeText(MainActivity.this,
                            getString(R.string.title_not_found),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BookObject> call, Throwable t) {
                loadingAnimation.animate().alpha(0.0f).setDuration(duration);
                Toast.makeText(MainActivity.this, "error :(", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reviewsFromIsbn() {
        BookIsbnReviewClient client = retrofit.create(BookIsbnReviewClient.class);
        Call<BookObject> call = client.getReviewsIsbn(isbnCode, API_KEY);

        call.enqueue(new Callback<BookObject>() {
            @Override
            public void onResponse(Call<BookObject> call, Response<BookObject> response) {
                //get values
                BookObject bookObject = response.body();
                if(bookObject != null) {
                    Book book = bookObject.getBook();
                    String title = String.format(getResources().getString(R.string.title_string), book.getTitle());
                    String rating = String.format(getResources().getString(R.string.rating_string), book.getRating());
                    String reviews = selectSubString(book.getReviewsWidget());
                    //process values
                    reviews = reviews.replace("565", screenWidth);
                    setupWebView(reviews);
                    titleTv.setText(title);
                    ratingTv.setText(rating);
                } else {
                    loadingAnimation.animate().alpha(0.0f).setDuration(duration);
                    Toast.makeText(MainActivity.this,
                            getString(R.string.isbn_not_found),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BookObject> call, Throwable t) {
                loadingAnimation.animate().alpha(0.0f).setDuration(duration);
                Toast.makeText(MainActivity.this, "error :(", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File tempFile = null;
            try {
                tempFile = PhotoUtils.mCreateTempFile(this);
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            if (tempFile != null) {

                tempFilePath = tempFile.getAbsolutePath();

                Uri pictureUri = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY, tempFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            loadingAnimation.animate().alpha(1.0f).setDuration(duration);
            reviewsWebView.loadUrl("about:blank");
            reviewsWebView.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    loadingAnimation.animate().alpha(1.0f).setDuration(duration);
                }
            });
            imageBitmap = PhotoUtils.fixRotation(PhotoUtils.mResampleImage(tempFilePath, this), tempFilePath);
            image = FirebaseVisionImage.fromBitmap(imageBitmap);
            processBarcodeScan();
        }
    }

    private void processBarcodeScan() {
        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
                        for(FirebaseVisionBarcode barcode : barcodes){
                            isbnCode = barcode.getRawValue();
                            if(!TextUtils.isEmpty(isbnCode)){
                                reviewsFromIsbn();
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingAnimation.animate().alpha(0.0f).setDuration(duration);
                        Toast.makeText(MainActivity.this,
                                "Problem with code recognition" + e.toString(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String selectSubString(String raw) {
        String[] firstSplit  = raw.split("(?=<iframe)");
        String[] secondSplit  = firstSplit[1].split("(?<=</iframe>)");
        return secondSplit[0];
    }

    private void setupWebView(String rawData) {
        reviewsWebView.getSettings().setJavaScriptEnabled(true);
        reviewsWebView.loadData(rawData, "text/html", null);
        reviewsWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                loadingAnimation.animate().alpha(0.0f).setDuration(duration);
            }
        });
        reviewsWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.e("progress", String.valueOf(newProgress));
            }
        });
    }

    private String getScreenWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int screenWidth = displayMetrics.widthPixels;
        float density = this.getResources().getDisplayMetrics().density;
        float dp = screenWidth/density;
        return String.valueOf(((int) dp) - 10);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode){
                case KeyEvent.KEYCODE_BACK:
                    if(reviewsWebView.canGoBack()) {reviewsWebView.goBack();
                    } else {
                        finish();
                    }
                    return  true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!TextUtils.isEmpty(tempFilePath)) {
            PhotoUtils.deleteFile(this, tempFilePath);
        }
    }
}
