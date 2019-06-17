package avikboss.compass;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class DataRecorder {

    private File workingFile;

    private FileOutputStream fOut;
    private OutputStreamWriter streamWriter;
    private String dir;
    private String ext;

    DataRecorder(String dir,String ext) {
        this.dir = "/"+dir+"/";
        this.ext = "."+ext;
    }

    private void createWorkingFile(String tag) {
        final File path =
                Environment.getExternalStoragePublicDirectory
                        (Environment.DIRECTORY_DOCUMENTS + dir);

        if (!path.exists()) {
            Log.i("WAS PATH MADE", Boolean.toString(path.mkdirs()));
        }
        workingFile = new File(path, "compass-" +tag +ext);
    }

    public void start(String tag) {
        createWorkingFile(tag);
        try {
            fOut = new FileOutputStream(workingFile);
        } catch (IOException e) {
            Log.e("File Error", Arrays.toString(e.getStackTrace()));
        }
        streamWriter = new OutputStreamWriter(fOut);
    }

    public void stop() {
        try {
            streamWriter.close();
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.e("File closing error", Arrays.toString(e.getStackTrace()));
        }
    }

    void write(String data) {

        try {
            streamWriter.append(data);
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}
