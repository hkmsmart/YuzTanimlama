package com.example.ahmethekim.yuztanimlama;

//... Uygulama içi kullanılacak kütüphaneleri ekliyoruz.
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//... Google vision api servis kütüphanelerini ekliyoruz
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

//... fotoğraf dosyası ile çalışacağımız için file kütüphanesini ekliyoruz.
import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {

    //... Burda Apiden error dönerse LOG_TAG dizisinde tutacaktır.
    private static final String LOG_TAG = "FACE API";
    //... En fazla 10 tane fotoğraf talebi göndericeğimizi belirtiyoruz.
    private static final int PHOTO_REQUEST = 10;
    //... Form objelerimiz tanımlıyoruz
    private TextView scanResults;
    private ImageView imageView;
    private Uri imageUri;
    private FaceDetector detector;
    //... Yetki url çizim gibi eklediğimiz kütüphanleri kullanıcağımız değişkenleri tanımlıyoruz.
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_BITMAP = "bitmap";
    private static final String SAVED_INSTANCE_RESULT = "result";
    Bitmap editedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //... Form objelerimiz tanımlıyoruz
        Button button = (Button) findViewById(R.id.button);
        scanResults   = (TextView) findViewById(R.id.results);
        imageView     = (ImageView) findViewById(R.id.scannedResults);

        if (savedInstanceState != null) {
            editedBitmap = savedInstanceState.getParcelable(SAVED_INSTANCE_BITMAP);
            if (savedInstanceState.getString(SAVED_INSTANCE_URI) != null) {
                imageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            }
            imageView.setImageBitmap(editedBitmap);
            scanResults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
        }
        //... Yüz tanıma objesini oluşturuyoruz. İzlemeyi Etkinleştir, işaret türlerini ayarla,İşlem türü ise sınıfı ayarla
        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
        //... Tıklama Yapıcısında Ayarla
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            }
        });
    }

    //...Onclick dinleyicisi a istekleri izin isteyin
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
                } else {
                    Toast.makeText(MainActivity.this, "Yetki Hatası !", Toast.LENGTH_SHORT).show();
                }
        }
    }

    //...Etkinlik Sonucu
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
            launchMediaScanIntent();
            try {
                scanFaces();
            } catch (Exception e) {
                Toast.makeText(this, "Fotoğraf yüklenirken hata oluştu", Toast.LENGTH_SHORT).show();
                Log.e(LOG_TAG, e.toString());
            }
        }
    }
    //... Apiden dönen sonuç yüz taraması
    private void scanFaces() throws Exception {
        Bitmap bitmap = decodeBitmapUri(this, imageUri);

        //... Eğer Apiden dönen sonuc true ise
        if (detector.isOperational() && bitmap != null) {
            editedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap .getHeight(), bitmap.getConfig());
            float scale  = getResources().getDisplayMetrics().density;

            //... Gözler ve burunun yüz çerçevesi çizimi için paint kütüphanesinin çağrılması
            Paint paint  = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.rgb(255, 61, 61));
            paint.setTextSize((int) (14 * scale));
            paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);

            Canvas canvas = new Canvas(editedBitmap);
            canvas.drawBitmap(bitmap, 0, 0, paint);
            Frame frame = new Frame.Builder().setBitmap(editedBitmap).build();
            SparseArray<Face> faces = detector.detect(frame);
            scanResults.setText(null);

            //... Apiden dönen sonuc ile foğoraflardaki sonuçları ekrana yadırma
            for (int index = 0; index < faces.size(); ++index) {
                Face face = faces.valueAt(index);
                //... Gözler ve burnun kordinatlarına göre çizim haritası oluşturmak.
                canvas.drawRect(
                        face.getPosition().x,
                        face.getPosition().y,
                        face.getPosition().x + face.getWidth(),
                        face.getPosition().y + face.getHeight(), paint);

                //... Apiden dönen sonuça göre Gülümseme olasılık, sol göz açıklığı ve sağ göz açıklığı
                scanResults.setText(scanResults.getText() + "Algılanan Yüz :" + (index + 1) + "\n");
                scanResults.setText(scanResults.getText() + "Mutluluk Olasılığı (1 ila 0 arasında) :" + "\n");
                scanResults.setText(scanResults.getText() + String.valueOf(face.getIsSmilingProbability()) + "\n");
                scanResults.setText(scanResults.getText() + "Sol Göz Açıklık Oranı (1 ila 0 arasında) : " + "\n");
                scanResults.setText(scanResults.getText() + String.valueOf(face.getIsLeftEyeOpenProbability()) + "\n");
                scanResults.setText(scanResults.getText() + "Sağ Göz Açıklık Oranı (1 ila 0 arasında): " + "\n");
                scanResults.setText(scanResults.getText() + String.valueOf(face.getIsRightEyeOpenProbability()) + "\n");
                scanResults.setText(scanResults.getText() + "---------" + "\n");

                for (Landmark landmark : face.getLandmarks()) {
                    int cx = (int) (landmark.getPosition().x);
                    int cy = (int) (landmark.getPosition().y);
                    canvas.drawCircle(cx, cy, 5, paint);
                }
            }
            //... Fotoğraf algılanamasa Apiden dönen sonuca göre ekranda mesajı yazdırıyoruz.
            //... else 'de for döngüsünün bitiminde alıglanan toplam yüzü yazdırıyoruz.'
            if (faces.size() == 0) {
                scanResults.setText("Fotoğrafda yüz bulunamadı.");
            }
            else {
                imageView.setImageBitmap(editedBitmap);
                scanResults.setText(scanResults.getText() + "Toplam Algınan Yüz: " + "\n");
                scanResults.setText(scanResults.getText() + String.valueOf(faces.size()) + "\n");
                scanResults.setText(scanResults.getText() + "---------" + "\n");
            }
        }
        //Eğer Apiden dönen sonuc false ise
        else {
            scanResults.setText("Api başlatılamadı!");
        }
    }
    //... Fotoğraf çekme ve Apiye gönderme
    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "picture.jpg");
        imageUri = FileProvider.getUriForFile(MainActivity.this,
                BuildConfig.APPLICATION_ID + ".provider", photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, PHOTO_REQUEST);
    }

    //... Ön izleme  Durumunu Kaydet
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (imageUri != null) {
            outState.putParcelable(SAVED_INSTANCE_BITMAP, editedBitmap);
            outState.putString(SAVED_INSTANCE_URI, imageUri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, scanResults.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detector.release();
    }
    //... Fotoğraf taramayı başlatma
    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    //... Bitmap kütüphanesini çağırma ve canvas oluşturma
    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }
}
