package com.vimalcvs.pdfreader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.theartofdev.edmodo.cropper.CropImage;
import com.vimalcvs.pdfreader.adapter.AdapterGridBasic;
import com.vimalcvs.pdfreader.adapter.ImageDocument;
import com.vimalcvs.pdfreader.adapter.SpacingItemDecoration;
import com.vimalcvs.pdfreader.helper.FileComparator;
import com.vimalcvs.pdfreader.helper.ImageToPDFAsync;
import com.vimalcvs.pdfreader.helper.ItemTouchHelperClass;
import com.vimalcvs.pdfreader.helper.RecyclerViewEmptySupport;
import com.vimalcvs.pdfreader.helper.ViewAnimation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageToPDF extends AppCompatActivity {

    private AdapterGridBasic mAdapter;
    private static final int READ_REQUEST_CODE = 42;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_COLLAGE = 265;

    public static ArrayList<ImageDocument> documents = null;
    ImageToPDF mainActivity;
    int currenSelected = -1;

    private CircularProgressBar progressBar;
    private TextView progressBarPercentage;
    private TextInputLayout textInputLayout;
    private EditText passwordText;
    AppCompatCheckBox securePDF;
    public ItemTouchHelper itemTouchHelper;
    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;
    LinearLayout mParentFloatButton;

    FloatingActionButton maddFilesFAB;
    private boolean rotate = false;
    String mCurrentCameraFile;
    private Dialog bottomSheetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_to_pdf);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initComponent();

        InitFabButtons();

        FloatingActionButton mConvertToPDF = findViewById(R.id.converttopdf);
        mConvertToPDF.setOnClickListener(view -> convertToPDF());

        mainActivity = this;
        InitBottomSheetProgress();
        actionModeCallback = new ActionModeCallback();
    }

    private void InitFabButtons() {
        maddFilesFAB = findViewById(R.id.addFilesFAB);
        mParentFloatButton = findViewById(R.id.parentfloatbutton);

        ViewAnimation.initShowOut(maddFilesFAB);
        FloatingActionButton mAddPDFFAB = findViewById(R.id.fabadd);

        mAddPDFFAB.setOnClickListener(v -> {
            rotate = ViewAnimation.rotateFab(v, !rotate);
            if (rotate) {
                ViewAnimation.showIn(maddFilesFAB);
            } else {
                ViewAnimation.showOut(maddFilesFAB);
            }
        });
        maddFilesFAB.setOnClickListener(v -> performFileSearch());

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void convertToPDF() {
        if (documents.size() < 1) {
            Toast.makeText(this, "You need to add at least 1 image file", Toast.LENGTH_LONG).show();
        } else {
            final Dialog dialog = new Dialog(mainActivity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            @SuppressLint("InflateParams")
            final View alertView = getLayoutInflater().inflate(R.layout.file_alert_dialog, null);

            textInputLayout = alertView.findViewById(R.id.editTextPassword);
            passwordText = alertView.findViewById(R.id.password);
            securePDF = alertView.findViewById(R.id.securePDF);
            securePDF.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    textInputLayout.setVisibility(View.VISIBLE);
                } else {
                    textInputLayout.setVisibility(View.GONE);
                }

            });
            final AppCompatSpinner spn_timezone = alertView.findViewById(R.id.pageorientation);

            String[] timezones = new String[]{"Portrait", "Landscape"};
            ArrayAdapter<String> array = new ArrayAdapter<>(mainActivity, R.layout.simple_spinner_item, timezones);
            array.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            spn_timezone.setAdapter(array);
            spn_timezone.setSelection(0);

            final AppCompatSpinner pageSize = alertView.findViewById(R.id.pagesize);

            String[] sizes = new String[]{"Fit (Same page size as image)", "A4 (297x210 mm)", "US Letter (215x279.4 mm)"};
            ArrayAdapter<String> pagearrary = new ArrayAdapter<>(mainActivity, R.layout.simple_spinner_item, sizes);
            pagearrary.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            pageSize.setAdapter(pagearrary);
            pageSize.setSelection(0);

            final AppCompatSpinner pageMargin = alertView.findViewById(R.id.margin);
            String[] margins = new String[]{"No margin", "Small", "Big"};
            ArrayAdapter<String> marginArray = new ArrayAdapter<>(mainActivity, R.layout.simple_spinner_item, margins);
            marginArray.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            pageMargin.setAdapter(marginArray);
            pageMargin.setSelection(0);

            final AppCompatSpinner compression = alertView.findViewById(R.id.compression);
            String[] compressions = new String[]{"Low", "Medium", "High"};
            ArrayAdapter<String> compressionArray = new ArrayAdapter<>(mainActivity, R.layout.simple_spinner_item, compressions);
            compressionArray.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            compression.setAdapter(compressionArray);
            compression.setSelection(2);

            final EditText edittext = alertView.findViewById(R.id.editText2);
            dialog.setContentView(alertView);
            dialog.setCancelable(true);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.show();
            dialog.getWindow().setAttributes(lp);
            dialog.findViewById(R.id.bt_close).setOnClickListener(v -> dialog.dismiss());

            dialog.findViewById(R.id.bt_save).setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    CheckStoragePermission();

                } else {
                    String fileName = edittext.getText().toString();
                    if (!fileName.equals("")) {

                        ImageToPDFAsync converter = new ImageToPDFAsync(mainActivity, documents, fileName, null);
                        if (securePDF.isChecked()) {
                            String password = passwordText.getText().toString();
                            converter.setPassword(password);
                        }
                        converter.setPageOrientation(spn_timezone.getSelectedItem().toString());
                        converter.setPageMargin(pageMargin.getSelectedItem().toString());
                        converter.setPageSize(pageSize.getSelectedItem().toString());
                        converter.setCompression(compression.getSelectedItem().toString());
                        converter.execute();
                        dialog.dismiss();
                    } else {
                        Snackbar.make(v, "File name should not be empty", Snackbar.LENGTH_LONG).show();
                    }
                }

            });

        }
    }

    private void initComponent() {
        documents = new ArrayList<>();
        RecyclerViewEmptySupport recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setEmptyView(findViewById(R.id.toDoEmptyView));
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setItemAnimator(null);
        recyclerView.addItemDecoration(new SpacingItemDecoration(3, dpToPx(this, 2), true));
        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    if (mParentFloatButton.getVisibility() == View.VISIBLE)
                        mParentFloatButton.setVisibility(View.GONE);
                } else if (dy < 0) {
                    if (mParentFloatButton.getVisibility() != View.VISIBLE)
                        mParentFloatButton.setVisibility(View.VISIBLE);
                }
            }
        });
        mAdapter = new AdapterGridBasic(this, documents);
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new AdapterGridBasic.OnItemClickListener() {
            @Override
            public void onItemClick(View view, ImageDocument obj, int position) {
                if (mAdapter.getSelectedItemCount() > 0) {
                    enableActionMode(position);
                } else {
                    currenSelected = position;
                    CropImage.activity(mAdapter.getItem(position).getImageDocument()).start(mainActivity);
                }
            }
            @Override
            public void onItemLongClick(View view, ImageDocument obj, int pos) {
                enableActionMode(pos);
            }
        });

        mAdapter.setDragListener(viewHolder -> {
            if (actionMode == null)
                itemTouchHelper.startDrag(viewHolder);
        });
        ItemTouchHelper.Callback callback = new ItemTouchHelperClass(mAdapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (result != null) {
                if (result.getClipData() != null) {
                    int count = result.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = result.getClipData().getItemAt(i).getUri();
                        ImageDocument document = new ImageDocument(imageUri, this);
                        addToDataStore(document);
                    }
                } else if (result.getData() != null) {
                    Uri imageUri = result.getData();
                    ImageDocument document = new ImageDocument(imageUri, this);
                    addToDataStore(document);
                }
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult cropped = CropImage.getActivityResult(result);
            if (resultCode == RESULT_OK) {
                Uri resultUri = cropped.getUri();
                if (currenSelected != -1) {
                    ImageDocument document = documents.get(currenSelected);
                    document.setImageDocument(resultUri);
                    mAdapter.notifyItemChanged(currenSelected);
                } else {
                    ImageDocument document = new ImageDocument(resultUri, getApplicationContext());
                    addToDataStore(document);
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            }
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            File file = new File(mCurrentCameraFile);
            if (file.exists()) {
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(mCurrentCameraFile));
                CropImage.activity(uri)
                        .start(this);
            }
        }
        if (requestCode == REQUEST_COLLAGE && resultCode == Activity.RESULT_OK) {
            makeResult();
        }
    }

    private void addToDataStore(ImageDocument item) {
        documents.add(item);
        mAdapter.notifyItemInserted(documents.size() - 1);
    }

    public static int dpToPx(Context c, int dp) {
        Resources r = c.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }


    public void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/jpeg");
        String[] mimetypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private void InitBottomSheetProgress() {

        bottomSheetDialog = new Dialog(this);
        bottomSheetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        bottomSheetDialog.setContentView(R.layout.progressdialog);
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(bottomSheetDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        progressBar = bottomSheetDialog.findViewById(R.id.circularProgressBar);
        progressBarPercentage = bottomSheetDialog.findViewById(R.id.progressPercentage);
        bottomSheetDialog.getWindow().setAttributes(lp);
    }

    public void showBottomSheet(int size) {
        bottomSheetDialog.show();
        this.progressBar.setProgressMax(size);
        this.progressBar.setProgress(0);
    }

    @SuppressLint("SetTextI18n")
    public void setProgress(int progress, int total) {
        this.progressBar.setProgress(progress);
        int percentage = (progress * 100) / total;
        this.progressBarPercentage.setText(percentage + "%");
    }

    public void runPostExecution() {
        bottomSheetDialog.dismiss();
        progressBarPercentage.setText("0%");
        this.progressBar.setProgress(0);
        makeResult();
    }

    public void makeResult() {
        Intent i = new Intent();
        this.setResult(RESULT_OK, i);
        this.finish();
    }

    private void CheckStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Storage Permission");
                alertDialog.setMessage("Storage permission is required in order to " +
                        "provide PDF merge feature, please enable permission in app settings");
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

    @SuppressLint("NotifyDataSetChanged")
    private void deleteItems() {
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
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
            mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
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
                deleteItems();
                mode.finish();
                return true;
            }
            if (id == R.id.select_all) {
                selectAll();
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
    private MenuItem mainMenuItem;
    private boolean isChecked = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sortmenu, menu);
        mainMenuItem = menu.findItem(R.id.fileSort);
        return true;
    }

    Comparator<ImageDocument> comparator = null;

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nameSort:
                mainMenuItem.setTitle("Name");
                comparator = FileComparator.getNameFileComparator();
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            case R.id.modifiedSort:
                mainMenuItem.setTitle("Modified");
                comparator = FileComparator.getLastModifiedFileComparator();
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            case R.id.sizeSort:
                mainMenuItem.setTitle("Size");
                comparator = FileComparator.getSizeFileComparator();
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
                    comparator = FileComparator.getLastModifiedFileComparator();
                }
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private void sortFiles(Comparator<ImageDocument> comparator) {
        Collections.sort(mAdapter.items, comparator);
        mAdapter.notifyDataSetChanged();
    }
}
