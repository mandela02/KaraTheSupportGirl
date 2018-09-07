package karajolel.wayne.com.karathesupportgirl;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_CAPTURE_IMAGE = 100;
    public ImageView mImage;
    public Button mButton;
    public TextView mTextDescription;
    public String imageFilePath;
    public Bitmap imageBitmap;
    public VisionServiceClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        description();
        initView();
    }

    private void initView() {
        mImage = findViewById(R.id.image_taken);
        mButton = findViewById(R.id.button_take_a_pic);
        mButton.setOnClickListener(this);
        mTextDescription = findViewById(R.id.text_analyze);
    }

    private void openCameraIntent() {
        Intent pictureIntent = new Intent(
            MediaStore.ACTION_IMAGE_CAPTURE
        );
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(pictureIntent,
                REQUEST_CAPTURE_IMAGE);
        }
        /*Intent pictureIntent = new Intent(
            MediaStore.ACTION_IMAGE_CAPTURE
        );
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                    "karajolel.wayne.com.karathesupportgir.fileprovider", photoFile);
                pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                    photoURI);
                startActivityForResult(pictureIntent,
                    REQUEST_CAPTURE_IMAGE);
            }
        }*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CAPTURE_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                imageBitmap = (Bitmap) data.getExtras().get("data");
                //mImage.setImageBitmap(imageBitmap);
                //Glide.with(this).load(imageFilePath).into(mImage);
                saveToInternalStorage();
                doDescribe();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User Cancelled the action
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //save to external storage. uncomment provider in android Manifest to use;
    private File createImageFile() throws IOException {
        String timeStamp =
            new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir =
            getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
        );
        imageFilePath = image.getAbsolutePath();
        return image;
    }

    //save to internal Storage
    private void saveToInternalStorage() {
        ContextWrapper wrapper = new ContextWrapper(getApplicationContext());
        String timeStamp =
            new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File file = wrapper.getDir("Images", MODE_PRIVATE);
        file = new File(file, imageFileName + ".jpg");
        try {
            OutputStream stream = new FileOutputStream(file);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.flush();
            stream.close();
        } catch (IOException e) // Catch the exception
        {
            e.printStackTrace();
        }
        Uri savedImageURI = Uri.parse(file.getAbsolutePath());
        mImage.setImageURI(savedImageURI);
    }

    private void description() {
        if (client == null) {
            client = new VisionServiceRestClient(getString(R.string.subscription_key),
                getString(R.string.subscription_apiroot));
        }
    }

    public void doDescribe() {
        mButton.setEnabled(false);
        mTextDescription.setText("Describing...");
        try {
            new doRequest().execute();
        } catch (Exception e) {
            mTextDescription.setText("Error encountered. Exception is: " + e.toString());
        }
    }

    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        //send image to describe.
        AnalysisResult v = this.client.describe(inputStream, 1);
        String result = gson.toJson(v);
        Log.d("result", result);
        return result;
    }

    private class doRequest extends AsyncTask<String, String, String> {
        private Exception e = null;

        public doRequest() {
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                return process();
            } catch (Exception e) {
                this.e = e;    // Store error
            }
            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            mTextDescription.setText("");
            if (e != null) {
                mTextDescription.setText("Error: " + e.getMessage());
                this.e = null;
            }
            else
            {
                Gson gson = new Gson();
                AnalysisResult result = gson.fromJson(data, AnalysisResult.class);

                mTextDescription.append("Image format: " + result.metadata.format + "\n");
                mTextDescription.append("Image width: " + result.metadata.width + ", height:" + result.metadata.height + "\n");
                mTextDescription.append("\n");

                for (Caption caption: result.description.captions) {
                    mTextDescription.append("Caption: " + caption.text + ", confidence: " + caption.confidence + "\n");
                }
                mTextDescription.append("\n");

                for (String tag: result.description.tags) {
                    mTextDescription.append("Tag: " + tag + "\n");
                }
                mTextDescription.append("\n");

                mTextDescription.append("\n--- Raw Data ---\n\n");
                mTextDescription.append(data);
            }
            mButton.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_take_a_pic:
                openCameraIntent();
                break;
            default:
                break;
        }
    }
}
