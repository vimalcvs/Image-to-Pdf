package com.vimalcvs.pdfreader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.flexbox.BuildConfig;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vimalcvs.pdfreader.adapter.MainRecycleViewAdapter;
import com.vimalcvs.pdfreader.helper.FileComparator;
import com.vimalcvs.pdfreader.helper.RecyclerViewEmptySupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int Merge_Request_CODE = 42;
    private RecyclerViewEmptySupport recyclerView;
    List<File> items = null;
    private MainRecycleViewAdapter mAdapter;
    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;
    private static final int RQS_OPEN_DOCUMENT_TREE_ALL = 43;
    private BottomSheetDialog mBottomSheetDialog;
    private MainActivity currentActivity;
    private static final int RQS_OPEN_DOCUMENT_TREE = 24;
    private File selectedFile;
    Dialog ocrProgressdialog;
    SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CheckStoragePermission();

        mSharedPreferences = getSharedPreferences("configuration", MODE_PRIVATE);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> StartMergeActivity("FileSearch"));


        recyclerView = findViewById(R.id.mainRecycleView);
        recyclerView.setEmptyView(findViewById(R.id.toDoEmptyView));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            CheckStoragePermission();
        }

        CreateDataSource();
        actionModeCallback = new ActionModeCallback();
        currentActivity = this;
        InitBottomSheetProgress();

    }

    public void StartMergeActivity(String message) {
        Intent intent = new Intent(getApplicationContext(), ImageToPDF.class);
        intent.putExtra("ActivityAction", message);
        startActivityForResult(intent, Merge_Request_CODE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sortmenu, menu);
        mainMenuItem = menu.findItem(R.id.fileSort);
        return true;
    }

    private MenuItem mainMenuItem;
    private boolean isChecked = false;
    Comparator<File> comparator = null;

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nameSort:
                mainMenuItem.setTitle("Name");
                comparator = FileComparator.getNameComparator();
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            case R.id.modifiedSort:
                mainMenuItem.setTitle("Modified");
                comparator = FileComparator.getLastModifiedComparator();
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            case R.id.sizeSort:
                mainMenuItem.setTitle("Size");
                comparator = FileComparator.getSizeComparator();
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            case R.id.ordering:
                isChecked = !isChecked;
                if (isChecked) {
                    item.setIcon(R.drawable.ic_keyboard_arrow_up_black_24dp);
                } else {
                    item.setIcon(R.drawable.ic_keyboard_arrow_down_black_24dp);
                }
                if (comparator == null) {
                    comparator = FileComparator.getLastModifiedComparator();
                }
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void sortFiles(Comparator<File> comparator) {
        Collections.sort(mAdapter.items, comparator);
        mAdapter.notifyDataSetChanged();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        if (requestCode == Merge_Request_CODE && resultCode == Activity.RESULT_OK) {
            if (result != null) {
                CreateDataSource();
                mAdapter.notifyItemInserted(items.size() - 1);
            }
        }
        if (resultCode == RESULT_OK && requestCode == RQS_OPEN_DOCUMENT_TREE) {
            if (result != null) {
                Uri uriTree = result.getData();
                DocumentFile documentFile = DocumentFile.fromTreeUri(this, uriTree);
                if (selectedFile != null) {
                    assert documentFile != null;
                    DocumentFile newFile = documentFile.createFile("application/pdf", selectedFile.getName());
                    try {
                        assert newFile != null;
                        copy(selectedFile, newFile);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    selectedFile = null;
                    if (mBottomSheetDialog != null)
                        mBottomSheetDialog.dismiss();
                    Toast toast = Toast.makeText(this, "Copy files to: " + documentFile.getName(), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
        if (resultCode == RESULT_OK && requestCode == RQS_OPEN_DOCUMENT_TREE_ALL) {
            if (result != null) {
                List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
                Uri uriTree = result.getData();
                DocumentFile documentFile = DocumentFile.fromTreeUri(this, uriTree);
                for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                    File file = items.get(i);
                    assert documentFile != null;
                    DocumentFile newFile = documentFile.createFile("application/pdf", file.getName());
                    try {
                        assert newFile != null;
                        copy(file, newFile);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (actionMode != null)
                    actionMode.finish();

                assert documentFile != null;
                Toast toast = Toast.makeText(this, "Copy files to: " + documentFile.getName(), Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    private void CheckStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Storage Permission");
                alertDialog.setMessage("Storage permission is required in order to " +
                        "provide Image to PDF feature, please enable permission in app settings");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Settings",
                        (dialog, id) -> {
                            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                            startActivity(i);
                            dialog.dismiss();
                        });
                alertDialog.show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
            }
        }
    }

    public void copy(File selectedFile, DocumentFile newFile) throws IOException {
        try {
            OutputStream out = getContentResolver().openOutputStream(newFile.getUri());
            FileInputStream in = new FileInputStream(selectedFile.getPath());
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CreateDataSource() {
        items = new ArrayList<>();
        File root = getFilesDir();
        File myDir = new File(root + "/ImageToPDF");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        File[] files = myDir.listFiles();

        assert files != null;
        Arrays.sort(files, (file1, file2) -> {
            long result = file2.lastModified() - file1.lastModified();
            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        });

        items.addAll(Arrays.asList(files));

        mAdapter = new MainRecycleViewAdapter(items);
        mAdapter.setOnItemClickListener(new MainRecycleViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, File value, int position) {
                if (mAdapter.getSelectedItemCount() > 0) {
                    enableActionMode(position);
                } else {
                    showBottomSheetDialog(value);
                }
            }

            @Override
            public void onItemLongClick(View view, File obj, int pos) {
                enableActionMode(pos);
            }

        });

        recyclerView.setAdapter(mAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void deleteItems() {
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            File file = items.get(i);
            file.delete();
            mAdapter.removeData(selectedItemPositions.get(i));
        }
        mAdapter.notifyDataSetChanged();
    }

    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
        }
        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        // ItemTouchHelperClass.isItemSwipe = false;
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    private void selectAll() {
        mAdapter.selectAll();
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_mainactionmode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                showCustomDeleteAllDialog(mode);
                return true;
            }
            if (id == R.id.select_all) {
                selectAll();
                return true;
            }
            if (id == R.id.action_share) {
                shareAll();
                return true;
            }
            if (id == R.id.action_copyTo) {
                copyToAll();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelections();
            actionMode = null;
        }

    }

    public void showCustomDeleteAllDialog(final ActionMode mode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure want to delete the selected files?");
        builder.setPositiveButton("OK", (dialog, id) -> {
            deleteItems();
            mode.finish();
        });
        builder.setNegativeButton("Cancel", (dialog, id) -> {

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void shareAll() {
        Intent target = ShareCompat.IntentBuilder.from(this).getIntent();
        target.setAction(Intent.ACTION_SEND_MULTIPLE);
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        ArrayList<Uri> files = new ArrayList<>();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            File file = items.get(i);
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", file);
            files.add(contentUri);
        }
        target.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        target.setType("application/pdf");
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (target.resolveActivity(getPackageManager()) != null) {
            startActivity(target);
        }
        actionMode.finish();
    }

    private void copyToAll() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, RQS_OPEN_DOCUMENT_TREE_ALL);
    }


    @SuppressLint("QueryPermissionsNeeded")
    private void showBottomSheetDialog(final File currentFile) {

        @SuppressLint("InflateParams")
        final View view = getLayoutInflater().inflate(R.layout.sheet_list, null);
        view.findViewById(R.id.lyt_email).setOnClickListener(view17 -> {
            mBottomSheetDialog.dismiss();
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", currentFile);
            Intent target = new Intent(Intent.ACTION_SEND);
            target.setType("text/plain");
            target.putExtra(Intent.EXTRA_STREAM, contentUri);
            target.putExtra(Intent.EXTRA_SUBJECT, "Subject");
            startActivity(Intent.createChooser(target, "Send via Email..."));
        });

        view.findViewById(R.id.lyt_share).setOnClickListener(view16 -> {
            mBottomSheetDialog.dismiss();
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", currentFile);
            Intent target = ShareCompat.IntentBuilder.from(currentActivity).setStream(contentUri).getIntent();
            target.setData(contentUri);
            target.setType("application/pdf");
            target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (target.resolveActivity(getPackageManager()) != null) {
                startActivity(target);
            }
        });

        view.findViewById(R.id.lyt_rename).setOnClickListener(view15 -> {
            mBottomSheetDialog.dismiss();
            showCustomRenameDialog(currentFile);

        });

        view.findViewById(R.id.lyt_delete).setOnClickListener(view13 -> {
            mBottomSheetDialog.dismiss();
            showCustomDeleteDialog(currentFile);

        });

        view.findViewById(R.id.lyt_copyTo).setOnClickListener(view12 -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, RQS_OPEN_DOCUMENT_TREE);
            selectedFile = currentFile;
        });


        view.findViewById(R.id.lyt_openFile).setOnClickListener(view14 -> {
            mBottomSheetDialog.dismiss();
            Intent target = new Intent(Intent.ACTION_VIEW);
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", currentFile);
            target.setDataAndType(contentUri, "application/pdf");
            target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent intent = Intent.createChooser(target, "Open File");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
            }

        });
        mBottomSheetDialog = new BottomSheetDialog(this);
        mBottomSheetDialog.setContentView(view);

        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(dialog -> mBottomSheetDialog = null);
    }

    public void showCustomRenameDialog(final File currentFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.rename_layout, null);
        builder.setView(view);
        final EditText editText = view.findViewById(R.id.renameEditText2);
        editText.setText(currentFile.getName());
        builder.setTitle("Rename");
        builder.setPositiveButton("Rename", (dialog, id) -> {
            File root = getFilesDir();
            File file = new File(root + "/ImageToPDF", editText.getText().toString());
            currentFile.renameTo(file);
            dialog.dismiss();
            CreateDataSource();
            mAdapter.notifyItemInserted(items.size() - 1);
        });
        builder.setNegativeButton("Cancel", (dialog, id) -> {

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showCustomDeleteDialog(final File currentFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure want to delete this file?");
        builder.setPositiveButton("OK", (dialog, id) -> {
            currentFile.delete();
            CreateDataSource();
            mAdapter.notifyItemInserted(items.size() - 1);
        });
        builder.setNegativeButton("Cancel", (dialog, id) -> {

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void InitBottomSheetProgress() {

        ocrProgressdialog = new Dialog(this);
        ocrProgressdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ocrProgressdialog.setContentView(R.layout.progressdialog);
        ocrProgressdialog.setCancelable(false);
        ocrProgressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(ocrProgressdialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        ocrProgressdialog.getWindow().setAttributes(lp);
    }

}
