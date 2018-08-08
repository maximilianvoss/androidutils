package rocks.voss.androidutils.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rocks.voss.androidutils.AndroidUtilsConstants;
import rocks.voss.androidutils.R;
import rocks.voss.androidutils.database.ExportData;
import rocks.voss.androidutils.database.ExportDataSet;
import rocks.voss.androidutils.utils.TimeUtil;
import rocks.voss.androidutils.utils.ToastUtil;


public class ExportGoogleDriveActivity extends Activity {

    protected static final int REQUEST_CODE_SIGN_IN = 0;
    protected static final int REQUEST_CODE_OPEN_ITEM = 1;

    private DriveResourceClient mDriveResourceClient;
    private ExportData exportData = null;

    private TaskCompletionSource<DriveId> mOpenItemTaskSource;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exportData = (ExportData) getIntent().getExtras().getSerializable(AndroidUtilsConstants.EXPORT_GOOGLE_DRIVE_ACTIVITY_EXPORT_DATA);
        String filename = getIntent().getExtras().getString(AndroidUtilsConstants.EXPORT_GOOGLE_DRIVE_ACTIVITY_EXPORT_FILE_NAME);

        setContentView(R.layout.activity_export_google_drive);

        Resources res = getResources();

        String text;
        String timestamp = TimeUtil.getNow().format(DateTimeFormatter.ofPattern("dd.MM.YYYY-HH:mm:ss"));
        if (filename != null && !filename.equals("")) {
            if (!filename.contains("%1$s")) {
                text = filename;
            } else {
                text = String.format(filename, timestamp);
            }
        } else {
            text = String.format(res.getString(R.string.activity_export_gd_filename_export), timestamp);
        }
        EditText textfieldFilename = findViewById(R.id.filename);
        textfieldFilename.setText(text);

        Button createButton = findViewById(R.id.create);
        createButton.setOnClickListener(v -> startExport());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode != RESULT_OK) {
                    Log.e(this.getClass().toString(), "Sign-in failed.");
                    ToastUtil.createLongToast(this, "Sign-in failed");
                    finish();
                    return;
                }

                Task<GoogleSignInAccount> getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                if (getAccountTask.isSuccessful()) {
                    initializeDriveClient(getAccountTask.getResult());
                } else {
                    Log.e(this.getClass().toString(), "Sign-in failed.");
                    ToastUtil.createLongToast(this, "Sign-in failed");
                    finish();
                }
                break;
            case REQUEST_CODE_OPEN_ITEM:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(OpenFileActivityOptions.EXTRA_RESPONSE_DRIVE_ID);
                    mOpenItemTaskSource.setResult(driveId);
                } else {
                    mOpenItemTaskSource.setException(new RuntimeException("Unable to open file"));
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void startExport() {

        if (exportData == null) {
            Log.e(this.getClass().toString(), "Exporting data failed");
            ToastUtil.createLongToast(this, "Exporting data failed");
            throw new IllegalThreadStateException("Export data is null and shouldn't be empty");
        }

        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        requiredScopes.add(Drive.SCOPE_APPFOLDER);
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            initializeDriveClient(signInAccount);
        } else {
            GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(Drive.SCOPE_FILE)
                    .requestScopes(Drive.SCOPE_APPFOLDER)
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
            startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }

    private void initializeDriveClient(GoogleSignInAccount signInAccount) {
        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), signInAccount);
        writeFile(exportData);
    }

    private void writeFile(ExportData exportData) {
        EditText textfieldFilename = findViewById(R.id.filename);
        String filename = textfieldFilename.getText().toString();
        if (filename.equals("")) {
            return;
        }

        final Task<DriveFolder> rootFolderTask = mDriveResourceClient.getRootFolder();
        final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
        Tasks.whenAll(rootFolderTask, createContentsTask)
                .continueWithTask(task -> {
                            DriveFolder parent = rootFolderTask.getResult();
                            DriveContents contents = createContentsTask.getResult();
                            OutputStream outputStream = contents.getOutputStream();
                            try (Writer writer = new OutputStreamWriter(outputStream)) {
                                writeList(writer, exportData.getSeparator(), exportData.getNewLine(), exportData.getHeader());
                                for (ExportDataSet dataSet : exportData.getDataSets()) {
                                    writeList(writer, exportData.getSeparator(), exportData.getNewLine(), dataSet.getValues());
                                }
                            }

                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle(filename)
                                    .setMimeType("text/csv")
                                    .setStarred(true)
                                    .build();

                            return mDriveResourceClient.createFile(parent, changeSet, contents);
                        }
                )
                .addOnSuccessListener(this,
                        driveFile -> {
                            Log.d(this.getClass().toString(), "File created");
                            ToastUtil.createLongToast(this, "File created");
                            finish();
                        }
                )
                .addOnFailureListener(this, e -> {
                            Log.e(this.getClass().toString(), "Unable to create file", e);
                            finish();
                        }
                );
    }

    private void writeList(Writer writer, char separator, String newLine, List<String> list) throws IOException {
        for (int i = 0; i < list.size(); i++) {
            writer.write(list.get(i));
            if (i < list.size() - 1) {
                writer.write(separator);
            }
        }
        writer.write(newLine);
    }
}
