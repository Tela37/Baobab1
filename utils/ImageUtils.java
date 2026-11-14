package td.teladoum.baobab.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import td.teladoum.baobab.R;

/**
 * Utilitaire pour la gestion des images et des audios, propulsé par Glide pour le cache et le chargement distant/local.
 */
public class ImageUtils {

    public static String getAudioDuration(String audioPath) {
        if (audioPath == null || audioPath.trim().isEmpty()) return "0:00";
        try (android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever()) {
            retriever.setDataSource(audioPath);
            String timeStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (timeStr != null) {
                long timeMs = Long.parseLong(timeStr);
                long seconds = (timeMs / 1000) % 60;
                long minutes = (timeMs / (1000 * 60)) % 60;
                return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds);
            }
        } catch (Exception e) {
            // ignore
        }
        return "0:00";
    }

    public static String saveImageToInternalStorage(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            return null;
        }

        try {
            File dir = new File(context.getFilesDir(), "profile_images");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = "img_" + System.currentTimeMillis() + "_" + System.nanoTime() + ".jpg";
            File destinationFile = new File(dir, filename);

            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                return null;
            }

            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();

            return destinationFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String saveVideoToInternalStorage(Context context, Uri videoUri) {
        if (context == null || videoUri == null) {
            return null;
        }

        try {
            File dir = new File(context.getFilesDir(), "post_videos");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = "video_" + System.currentTimeMillis() + "_" + System.nanoTime() + ".mp4";
            File destinationFile = new File(dir, filename);

            InputStream inputStream = context.getContentResolver().openInputStream(videoUri);
            if (inputStream == null) {
                return null;
            }

            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.flush();
            outputStream.close();

            return destinationFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String saveFileToInternalStorage(Context context, Uri fileUri, String prefix) {
        if (context == null || fileUri == null) {
            return null;
        }

        try {
            File dir = new File(context.getFilesDir(), "document_files");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = (prefix != null ? prefix : "file") + "_" + System.currentTimeMillis() + "_" + System.nanoTime() + ".pdf";
            File destinationFile = new File(dir, filename);

            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                return null;
            }

            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.flush();
            outputStream.close();

            return destinationFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isBase64Image(String str) {
        if (str == null) return false;
        String trimmed = str.trim();
        return trimmed.startsWith("data:image/") || (trimmed.length() > 100 && !trimmed.startsWith("http") && !trimmed.startsWith("/"));
    }

    public static Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            if (base64Str == null) return null;
            String cleanBase64 = base64Str.trim();
            if (cleanBase64.contains(",")) {
                cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
            }
            byte[] decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            int maxDim = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxDim || height > maxDim) {
                float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
                width = Math.round(width * ratio);
                height = Math.round(height * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return "data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String uriToBase64(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            return bitmapToBase64(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isImagePathValid(Context context, String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return false;
        }

        String trimmed = imagePath.trim();

        if (isBase64Image(trimmed)) {
            return true;
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return true;
        }

        if (trimmed.startsWith("content://")) {
            if (context == null) return true;
            try {
                Uri uri = Uri.parse(trimmed);
                try (android.content.res.AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
                    return afd != null;
                }
            } catch (Exception e) {
                return false;
            }
        }

        String filePath = trimmed;
        if (filePath.startsWith("file://")) {
            filePath = filePath.substring(7);
        }

        if (filePath.startsWith("/")) {
            File file = new File(filePath);
            return file.exists() && file.isFile() && file.length() > 0;
        }

        if (trimmed.startsWith("android.resource://")) {
            return true;
        }

        return false;
    }

    public static boolean isVideoPathValid(Context context, String videoPath) {
        if (videoPath == null || videoPath.trim().isEmpty()) {
            return false;
        }

        String trimmed = videoPath.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return true;
        }

        if (trimmed.startsWith("content://")) {
            if (context == null) return true;
            try {
                Uri uri = Uri.parse(trimmed);
                try (android.content.res.AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
                    return afd != null;
                }
            } catch (Exception e) {
                return false;
            }
        }

        String filePath = trimmed;
        if (filePath.startsWith("file://")) {
            filePath = filePath.substring(7);
        }

        if (filePath.startsWith("/")) {
            File file = new File(filePath);
            return file.exists() && file.isFile() && file.length() > 0;
        }

        return false;
    }

    public static void loadProfileImage(Context context, String imagePath, ImageView imageView) {
        if (imageView == null) return;

        Context targetContext = context != null ? context : imageView.getContext();

        if (!isImagePathValid(targetContext, imagePath)) {
            imageView.setImageResource(R.drawable.ic_profile);
            return;
        }

        if (isBase64Image(imagePath)) {
            Bitmap b = decodeBase64ToBitmap(imagePath);
            if (b != null) {
                imageView.setImageBitmap(b);
            } else {
                imageView.setImageResource(R.drawable.ic_profile);
            }
            return;
        }

        if (targetContext == null) {
            imageView.setImageResource(R.drawable.ic_profile);
            return;
        }

        try {
            Glide.with(targetContext)
                    .load(imagePath)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.ic_profile);
        }
    }

    public static void loadFullImage(Context context, String imagePath, ImageView imageView) {
        if (imageView == null) return;

        Context targetContext = context != null ? context : imageView.getContext();

        if (!isImagePathValid(targetContext, imagePath)) {
            imageView.setImageDrawable(null);
            return;
        }

        if (isBase64Image(imagePath)) {
            Bitmap b = decodeBase64ToBitmap(imagePath);
            if (b != null) {
                imageView.setImageBitmap(b);
            } else {
                imageView.setImageDrawable(null);
            }
            return;
        }

        if (targetContext == null) {
            imageView.setImageDrawable(null);
            return;
        }

        try {
            Glide.with(targetContext)
                    .load(imagePath)
                    .placeholder(android.R.color.transparent)
                    .error(android.R.color.transparent)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageDrawable(null);
        }
    }

    public static void loadPostImage(Context context, String imagePath, ImageView imageView) {
        if (imageView == null) return;

        Context targetContext = context != null ? context : imageView.getContext();

        if (!isImagePathValid(targetContext, imagePath)) {
            imageView.setImageDrawable(null);
            return;
        }

        if (isBase64Image(imagePath)) {
            Bitmap b = decodeBase64ToBitmap(imagePath);
            if (b != null) {
                imageView.setImageBitmap(b);
            } else {
                imageView.setImageDrawable(null);
            }
            return;
        }

        if (targetContext == null) {
            imageView.setImageDrawable(null);
            return;
        }

        try {
            Glide.with(targetContext)
                    .load(imagePath)
                    .centerCrop()
                    .placeholder(R.drawable.ic_baobab_logo)
                    .error(R.drawable.ic_baobab_logo)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageDrawable(null);
        }
    }

    public static void loadStoryImage(Context context, String imagePath, ImageView imageView) {
        if (imageView == null) return;

        Context targetContext = context != null ? context : imageView.getContext();

        if (!isImagePathValid(targetContext, imagePath)) {
            imageView.setImageDrawable(null);
            return;
        }

        if (isBase64Image(imagePath)) {
            Bitmap b = decodeBase64ToBitmap(imagePath);
            if (b != null) {
                imageView.setImageBitmap(b);
            } else {
                imageView.setImageDrawable(null);
            }
            return;
        }

        if (targetContext == null) {
            imageView.setImageDrawable(null);
            return;
        }

        try {
            Glide.with(targetContext)
                    .load(imagePath)
                    .centerCrop()
                    .placeholder(R.drawable.badge_background)
                    .error(R.drawable.badge_background)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageDrawable(null);
        }
    }

    public static void loadVideoThumbnail(Context context, String videoPath, ImageView imageView) {
        if (imageView == null) return;

        Context targetContext = context != null ? context : imageView.getContext();

        if (!isVideoPathValid(targetContext, videoPath)) {
            imageView.setBackgroundColor(android.graphics.Color.parseColor("#333333"));
            imageView.setImageBitmap(null);
            return;
        }

        if (targetContext == null) {
            imageView.setBackgroundColor(android.graphics.Color.parseColor("#333333"));
            imageView.setImageBitmap(null);
            return;
        }

        try {
            Glide.with(targetContext)
                    .load(videoPath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } catch (Exception e) {
            imageView.setBackgroundColor(android.graphics.Color.parseColor("#333333"));
            imageView.setImageBitmap(null);
        }
    }

    /**
     * Active le zoom à deux doigts (Pinch-to-Zoom) sur un ImageView.
     */
    public static void enablePinchToZoom(ImageView imageView) {
        if (imageView == null) return;

        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(
                imageView.getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    private float scaleFactor = 1.0f;

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        scaleFactor *= detector.getScaleFactor();
                        scaleFactor = Math.max(0.8f, Math.min(scaleFactor, 4.0f));
                        imageView.setScaleX(scaleFactor);
                        imageView.setScaleY(scaleFactor);
                        return true;
                    }
                }
        );

        imageView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
    }

    public static void downloadImageToGallery(Context context, String imagePath) {
        if (context == null || imagePath == null || imagePath.trim().isEmpty()) return;
        try {
            File sourceFile = new File(imagePath);
            if (sourceFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(sourceFile.getAbsolutePath());
                if (bitmap != null) {
                    saveBitmapToDownloads(context, bitmap);
                    return;
                }
            }

            Glide.with(context)
                    .asBitmap()
                    .load(imagePath)
                    .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@androidx.annotation.NonNull Bitmap resource, com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                            saveBitmapToDownloads(context, resource);
                        }
                        @Override
                        public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}
                    });
        } catch (Exception e) {
            e.printStackTrace();
            es.dmoral.toasty.Toasty.error(context, "Erreur lors du téléchargement", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private static void saveBitmapToDownloads(Context context, Bitmap bitmap) {
        try {
            String filename = "BaobabTD_" + System.currentTimeMillis() + ".jpg";
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/BaobabTD");
            }

            Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                java.io.OutputStream out = context.getContentResolver().openOutputStream(uri);
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                    es.dmoral.toasty.Toasty.success(context, "Image téléchargée dans Téléchargements/BaobabTD 📥", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            es.dmoral.toasty.Toasty.error(context, "Erreur lors de l'enregistrement", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}

