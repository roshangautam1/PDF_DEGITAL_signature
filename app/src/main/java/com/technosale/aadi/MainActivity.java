package com.technosale.aadi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import com.simplify.ink.InkView;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;

public class MainActivity extends AppCompatActivity {
    private static final int OPEN_REQUEST_CODE = 12;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;
    private static final int MY_PERMISSIONS_REQUEST_W_EXTERNAL_STORAGE = 1234;
    private static final int MY_PERMISSIONS_REQUEST_M_EXTERNAL_STORAGE = 134;
    private InkView ink;
    private TextView mFileNameTextView;
    private Uri mFileUri;
    private ImageView mViewPDFImageView;
    private EditText mReasonEditText;
    private EditText mLocationEditText;
    private ProgressDialog mProgressDialog;
    private Button mSignButton;
    File copiedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissionAndBrowseFile();
        initView();
        final Activity a = this;
        findViewById(R.id.iv_clean).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ink.clear();
            }
        });
        mViewPDFImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFileUri != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(mFileUri);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "No PDF File!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (Build.VERSION.SDK_INT >= 30){
            if (!Environment.isExternalStorageManager()){
                Intent getpermission = new Intent();
                getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(getpermission);
            }
        }

        findViewById(R.id.iv_browse_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, OPEN_REQUEST_CODE);
            }
        });
        mSignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFileUri == null) {
                    Toast.makeText(getApplicationContext(), "Please choose a pdf file!", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveImage();
                KeyChain.choosePrivateKeyAlias(a,
                    new KeyChainAliasCallback() {
                        public void alias(String alias) {
                            // Credential alias selected.  Remember the alias selection for future use.
                            if (alias != null) {
                                Handler h = new Handler(Looper.getMainLooper());
                                h.post(new Runnable() {
                                    public void run() {
                                        showProgressDialog();
                                    }
                                });
                                sign(alias, mReasonEditText.getText().toString(), mLocationEditText.getText().toString());
                            }
                        }
                    }, null, null, null, -1, null);
            }
        });
    }

    private void initView() {
        ink = (InkView) findViewById(R.id.ink);
        mSignButton = (Button) findViewById(R.id.button_sign);
        mFileNameTextView = (TextView) findViewById(R.id.tv_file_name);
        mViewPDFImageView = (ImageView) findViewById(R.id.iv_view);
        mReasonEditText = (EditText) findViewById(R.id.et_reason);
        mLocationEditText = (EditText) findViewById(R.id.et_location);
        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OPEN_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    mFileUri = data.getData();
                    System.out.println(mFileUri+"iiiiiiiiiii");
                    System.out.println(mFileUri.getLastPathSegment()+"iiiiiiiiiii");
                    System.out.println(mFileUri.getPath()+"iiiiiiiiiii");
                    mFileNameTextView.setText(getFileName(mFileUri));
                    if (mFileUri != null) {
                        // Define the destination directory and the new file name
                        File destinationDir = getFilesDir(); // Use internal storage                        String newFileName = "sign_"+getFileName(mFileUri);

                        // Copy the selected PDF file to the new location
                        try {
                            copiedFile =  copyFile(mFileUri, destinationDir, "sign_"+getFileName(mFileUri));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    Toast.makeText(this, "File action canceled", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
    private File copyFile(Uri uri, File destinationDir, String newFileName) throws IOException {
        try (FileInputStream input = (FileInputStream) getContentResolver().openInputStream(uri)) {
            File outputFile = new File(destinationDir, newFileName);
            try (FileOutputStream output = new FileOutputStream(outputFile)) {
                FileChannel inputChannel = input.getChannel();
                FileChannel outputChannel = output.getChannel();
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                System.out.println("copiedcomsdofsefksjfsj11");
            }
            return outputFile;
        }
    }
    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    private void saveImage() {
        FileOutputStream out = null;
        try {
            System.out.println("saving the image");
//            out = new FileOutputStream(getFilesDir() + "/image.png");
//            ink.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void sign(final String alias, final String reason, final String location) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDialog.dismiss();
                if(copiedFile!= null){
                    Intent i = new Intent(getApplicationContext(), DoneActivity.class);
//                String f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/sign_" + getFileName(mFileUri);
                    i.putExtra("uri", copiedFile.getAbsolutePath());
                    startActivity(i);
                }
            }
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            protected Void doInBackground(Void... params) {
                PrivateKey privateKey;
                try {
                    privateKey = KeyChain.getPrivateKey(getApplicationContext(), alias);
                    KeyFactory keyStore =
                            KeyFactory.getInstance(privateKey.getAlgorithm(), "AndroidKeyStore");
                    Certificate[] chain = KeyChain.getCertificateChain(getApplicationContext(), alias);

                    BouncyCastleProvider provider = new BouncyCastleProvider();
                    Security.addProvider(provider);

                    ExternalSignature pks = new CustomPrivateKeySignature(privateKey, DigestAlgorithms.SHA256, provider.getName());

                    File tmp = File.createTempFile("eid", ".pdf", getCacheDir());
                    File file = new File(copiedFile.getAbsolutePath());
                    FileOutputStream fos = new FileOutputStream(file);
                    sign(mFileUri, fos, chain, pks, DigestAlgorithms.SHA256, provider.getName(), CryptoStandard.CADES, reason, location);

                } catch (KeyChainException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }
    public void sign(Uri uri, FileOutputStream os,
                     Certificate[] chain,
                     ExternalSignature pk, String digestAlgorithm, String provider,
                     CryptoStandard subfilter,
                     String reason, String location)
            throws GeneralSecurityException, IOException, DocumentException {
        try {
            // Creating the reader and the stamper
            PdfReader reader = new PdfReader(getContentResolver().openInputStream(uri));
//            PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');
            PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');
            // Creating the appearance
            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
            appearance.setReason(reason);
            appearance.setLocation(location);
            float bottomLeftX = 6; // Assuming 36 units margin from the left
            float bottomLeftY = 6; // Assuming 36 units margin from the bottom
            float width = 100;
            float height = 50;
            appearance.setVisibleSignature(new Rectangle(bottomLeftX, bottomLeftY, bottomLeftX + width, bottomLeftY + height), 1, "sig");
            try {
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.certification_logo);
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                Bitmap bitmap = bitmapDrawable.getBitmap();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                Image image = Image.getInstance(byteArray);
                if(image!= null){
                    appearance.setImage(image);
                }
            } catch (BadElementException | IOException e) {
                e.printStackTrace();
                // Handle the exception appropriately
            }
            appearance.setImageScale(-1);
            // Creating the signature
            ExternalDigest digest = new BouncyCastleDigest();
            CustomMakeSignature.signDetached(appearance, digest, pk, chain, null, null, null, 0, subfilter);
            stamper.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    private void showProgressDialog() {
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle("Signing documents");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("please wait a moment");
        mProgressDialog.show();
    }
    private void checkPermissionAndBrowseFile() {
        // Check if the READ_EXTERNAL_STORAGE permission is not granted yet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_W_EXTERNAL_STORAGE);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_M_EXTERNAL_STORAGE);
        } else {
            // Permission is already granted, perform file browsing
            browseFile();
        }
    }

    private void browseFile() {
        // Implement your file browsing logic here
        // You can launch the file picker intent or perform any other file-related operations
        Toast.makeText(this, "File browsing logic goes here", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, perform file browsing
                browseFile();
            } else {
                // Permission denied, handle accordingly (e.g., show a message to the user)
                Toast.makeText(this, "Permission denied. Cannot browse files.", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    private void generatePageForDigitalSignature() throws IOException {
//        int pageHeight = 1120;
//        int pagewidth = 792;
//
//        // creating a bitmap variable
//        // for storing our images
//        Bitmap bmp, scaledbmp;
//        if(imagePath!=null){
//
//            bmp = BitmapFactory.decodeResource(getResources(), R.drawable.certification_logo);
//            scaledbmp = Bitmap.createScaledBitmap(bmp, 640, 340, false);
//
////        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.yy);
//        // creating an object variable
//        // for our PDF document.
////        File file = new File( getFilesDir() + "/sign_" + getFileName(mFileUri));
////        Uri pdfUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileproviders", file);
////        System.out.println(pdfUri+"oooo111o1oo1o1o1oo1o1");
//
//
//        PdfDocument pdfDocument = new PdfDocument();
////        PdfReader reader = new PdfReader(extractActualPath(mFileUri.getLastPathSegment()));
////        PdfReader reader = new PdfReader(getFilesDir()+getFileName(mFileUri));
////        int n = reader.getNumberOfPages();
//
//        // two variables for paint "paint" is used
//        // for drawing shapes and we will use "title"
//        // for adding text in our PDF file.
//        Paint paint = new Paint();
//        Paint context = new Paint();
//
//        // we are adding page info to our PDF file
//        // in which we will be passing our pageWidth,
//        // pageHeight and number of pages and after that
//        // we are calling it to create our PDF.
//        PdfDocument.PageInfo mypageInfo = new PdfDocument.PageInfo.Builder(pagewidth, pageHeight, 1).create();
//
//        // below line is used for setting
//        // start page for our PDF file.
//        PdfDocument.Page myPage = pdfDocument.startPage(mypageInfo);
//
//        // creating a variable for canvas
//        // from our page of PDF.
//        Canvas canvas = myPage.getCanvas();
//
//        // below line is used to draw our image on our PDF file.
//        // the first parameter of our drawbitmap method is
//        // our bitmap
//        // second parameter is position from left
//        // third parameter is position from top and last
//        // one is our variable for paint.
//        canvas.drawBitmap(scaledbmp, 56, 40, paint);
//
//        // below line is used for adding typeface for
//        // our text which we will be adding in our PDF file.
//        context.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
//
//        // below line is used for setting text size
//        // which we will be displaying in our PDF file.
//        context.setTextSize(15);
//
//        // below line is sued for setting color
//        // of our text inside our PDF file.
//        context.setColor(ContextCompat.getColor(this, R.color.purple_200));
//
//        // below line is used to draw text in our PDF file.
//        // the first parameter is our text, second parameter
//        // is position from start, third parameter is position from top
//        // and then we are passing our variable of paint which is title.
//             context.setTextAlign(Paint.Align.CENTER);
//        canvas.drawText("Digitally signed by"+mReasonEditText.getText().toString(), 209, 100, context);
//        canvas.drawText("DN:c=NP, o=Nepal Certifying Company Pvt Ltd, ou=IT, cn="+mReasonEditText.getText().toString()+",address="+mLocationEditText.getText().toString()+"email=,Date="+new Date().getTime(), 209, 80, context);
//        pdfDocument.finishPage(myPage);
//        // after storing our pdf to that
//        // location we are closing our PDF file.
//            File pdfFile = new File(getFilesDir(), "generated_pdf.pdf");
//            try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
//                pdfDocument.writeTo(outputStream);
//            }
//        pdfDocument.close();
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            Uri pdfUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", pdfFile);
//            intent.setDataAndType(pdfUri, "application/pdf");
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//            // Check if there is a PDF viewer available
//            if (intent.resolveActivity(getPackageManager()) != null) {
//                // Open the PDF file with the default PDF viewer
//                startActivity(intent);
//            } else {
//                // Notify the user that there is no PDF viewer available
//                Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

}
