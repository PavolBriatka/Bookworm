package com.briatka.pavol.bookworm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class PhotoUtils {

    static File mCreateTempFile(Context context) throws IOException {
        String fileName = new SimpleDateFormat("E-dd-MM-yyyy", Locale.getDefault())
                .format(new Date());

        File storageDir = context.getExternalCacheDir();

        return File.createTempFile(
                fileName,
                ".jpg",
                storageDir
        );
    }

    static Bitmap mResampleImage(String filePath, Context context) {

        //get device screen size
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);

        int deviceWidth = metrics.widthPixels;
        int deviceHeight = metrics.heightPixels;

        //get photo size without allocating memory
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        int photoWidth = options.outWidth;
        int photoHeight = options.outHeight;


        //calculate inSampleSize
        int inSampleSize = Math.min(photoWidth / deviceHeight, photoHeight / deviceWidth);


        //decode resized picture
        options.inSampleSize = inSampleSize;

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);

    }

    static Bitmap fixRotation(Bitmap bitmapPicture, String filepath) {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Matrix matrix = new Matrix();

        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270);
                break;
            default:
        }

        return Bitmap.createBitmap(
                bitmapPicture,
                0,
                0,
                bitmapPicture.getWidth(),
                bitmapPicture.getHeight(),
                matrix,
                true);

    }

    static boolean deleteFile(Context context, String filePath) {

        File imageFile = new File(filePath);

        boolean isDeleted = imageFile.delete();

        if (!isDeleted) {
            Toast.makeText(context, "The file path does not exist anymore",
                    Toast.LENGTH_SHORT).show();
        }
        return isDeleted;
    }

}
