package com.briatka.pavol.bookworm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
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

import net.cachapa.expandablelayout.ExpandableLayout;

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
    @BindView(R.id.info_layout)
    LinearLayout infoLayout;
    @BindView(R.id.rating_bar)
    RatingBar ratingBar;
    @BindView(R.id.main_card_view)
    CardView mainCardView;
    @BindView(R.id.expandable_layout)
    ExpandableLayout expandableLayout;

    static final int REQUEST_IMAGE_CAPTURE = 27;
    private static final String API_KEY = "RrXbLty3WjyNPa58H93Rdw";
    private static final String URL = "https://www.goodreads.com";
    private static final String FILE_PROVIDER_AUTHORITY = "com.briatka.pavol.bookworm.fileprovider";

    Bitmap imageBitmap;
    String tempFilePath;
    String mScreenWidth;
    String mScreenHeight;
    String isbnCode;
    int shortDuration = 500;
    int longDuration = 1000;

    FirebaseVisionImage image;
    FirebaseVisionTextRecognizer textRecognizer;
    FirebaseVisionBarcodeDetector detector;

    Retrofit retrofit;

    @Override
    public void sendInput(String input) {
        if(reviewsWebView.getAlpha() > 0) reviewsWebView.animate().alpha(0.0f).setDuration(longDuration);
        if(infoLayout.getAlpha() > 0) infoLayout.animate().alpha(0.0f).setDuration(shortDuration);
        if(expandableLayout.isExpanded()) expandableLayout.collapse();
        loadingAnimation.animate().alpha(1.0f).setDuration(shortDuration);
        reviewsFromTitle(input);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        getScreenDimens();
        textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector();


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
                //process values
                processNetworkResponse(bookObject);
            }

            @Override
            public void onFailure(Call<BookObject> call, Throwable t) {
                loadingAnimation.animate().alpha(0.0f).setDuration(shortDuration);
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
                //process values
                processNetworkResponse(bookObject);
            }

            @Override
            public void onFailure(Call<BookObject> call, Throwable t) {
                loadingAnimation.animate().alpha(0.0f).setDuration(shortDuration);
                Toast.makeText(MainActivity.this, "error :(", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processNetworkResponse(BookObject bookObject) {
        if(bookObject != null) {
            Book book = bookObject.getBook();
            String reviews = selectSubString(book.getReviewsWidget());
            reviews = reviews.replace("565", mScreenWidth);
            setupWebView(reviews);
            titleTv.setText(book.getTitle());
            ratingBar.setRating(Float.parseFloat(book.getRating()));
        } else {
            loadingAnimation.animate().alpha(0.0f).setDuration(shortDuration);
            Toast.makeText(MainActivity.this,
                    getString(R.string.isbn_not_found),
                    Toast.LENGTH_SHORT).show();
        }
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
            loadingAnimation.animate().alpha(1.0f).setDuration(shortDuration);
            if(reviewsWebView.getAlpha() > 0) reviewsWebView.animate().alpha(0.0f).setDuration(longDuration);
            if(infoLayout.getAlpha() > 0) infoLayout.animate().alpha(0.0f).setDuration(longDuration);
            if(expandableLayout.isExpanded()) expandableLayout.collapse();
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
                        loadingAnimation.animate().alpha(0.0f).setDuration(shortDuration);
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
                loadingAnimation.animate().alpha(0.0f).setDuration(shortDuration);
                reviewsWebView.animate().alpha(1.0f).setDuration(longDuration);
                expandableLayout.expand();
                infoLayout.animate().alpha(1.0f).setDuration(longDuration);
            }
        });
    }

    private void getScreenDimens() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        float density = this.getResources().getDisplayMetrics().density;
        float dpWidth = screenWidth/density;
        float dpHeight = screenHeight/density;
        mScreenWidth = String.valueOf(((int) dpWidth) - 10);
        mScreenHeight = String.valueOf(((int) dpHeight));
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
