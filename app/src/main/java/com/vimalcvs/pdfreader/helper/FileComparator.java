package com.vimalcvs.pdfreader.helper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import com.vimalcvs.pdfreader.adapter.ImageDocument;

import java.io.File;
import java.util.Comparator;

public class FileComparator {

    public static Comparator<ImageDocument> getLastModifiedFileComparator() {
        return (file1, file2) -> {
            long result;
            if (!isDescending)
                result = file2.getLastModified() - file1.getLastModified();
            else
                result = file1.getLastModified() - file2.getLastModified();
            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        };
    }

    public static Comparator<File> getLastModifiedComparator() {
        return (file1, file2) -> {
            long result;
            if (!isDescending)
                result = file2.lastModified() - file1.lastModified();
            else
                result = file1.lastModified() - file2.lastModified();
            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        };
    }

    public static Comparator<ImageDocument> getSizeFileComparator() {
        return (file1, file2) -> {
            long size1;
            size1 = file1.getSize();
            long size2;
            size2 = file2.getSize();
            long result;
            if (!isDescending)
                result = size1 - size2;
            else
                result = size2 - size1;
            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        };

    }

    public static Comparator<File> getSizeComparator() {
        return (file1, file2) -> {
            long size1;
            size1 = file1.length();
            long size2;
            size2 = file2.length();
            long result;
            if (!isDescending)
                result = size1 - size2;
            else
                result = size2 - size1;
            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        };

    }

    public static boolean isDescending = false;

    public static Comparator<ImageDocument> getNameFileComparator() {
        return (f1, f2) -> {
            if (!isDescending)
                return f1.getFileName().compareTo(f2.getFileName());
            else
                return f2.getFileName().compareTo(f1.getFileName());
        };
    }

    public static Comparator<File> getNameComparator() {
        return (f1, f2) -> {
            if (!isDescending)
                return f1.getName().compareTo(f2.getName());
            else
                return f2.getName().compareTo(f1.getName());
        };
    }

    public static String getPath(final Context context, final Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }

            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception ignored) {

        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
