package com.technosale.aadi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DoneActivity extends AppCompatActivity {
    private Uri mSignedFileUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
        setContentView(R.layout.activity_done);

        mSignedFileUri =  FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileproviders", new File(getIntent().getStringExtra("uri")));
        findViewById(R.id.button_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("clicked on the imageview");
                openFile();
            }
        });

        findViewById(R.id.button_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("pdf/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, mSignedFileUri);
                startActivity(Intent.createChooser(shareIntent, "Share Document"));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void openFile() {
        if (mSignedFileUri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(mSignedFileUri, "application/pdf");
            intent.addFlags( Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION );
            intent.setData(mSignedFileUri);

            openFileFromContentUri(mSignedFileUri);
            if (intent.resolveActivity(getPackageManager()) != null) {
                System.out.println("wintered to the transaction");
//                /data/user/0/com.technosale.aadi/files/sign_dbms.pdf
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "No-PDF viewer app installed!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No-PDF File!", Toast.LENGTH_SHORT).show();
        }
    }


    // Assume mFileUri is a member variable holding the Uri of the file
    public void openFileFromContentUri(Uri contentUri) {
        InputStream inputStream = null;
        try {
            // Open an InputStream from the ContentResolver
            inputStream = getContentResolver().openInputStream(contentUri);

            // Now you can read from the InputStream or perform any operations on the file content
            // For example, you can read the content line by line using a BufferedReader
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            // Do something with the file content (e.g., display it, parse it, etc.)
            String fileContent = content.toString();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle exceptions appropriately (e.g., show an error message)
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}