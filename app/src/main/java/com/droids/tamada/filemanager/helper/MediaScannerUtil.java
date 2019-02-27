/*
 * Copyright (c) 2019. Parrot Faurecia Automotive S.A.S. All rights reserved.
 */

package com.droids.tamada.filemanager.helper;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;

/**
 * Call this util to notify Media library to scan designated mFiles and update database.
 */
public class MediaScannerUtil implements MediaScannerConnection.MediaScannerConnectionClient{

    /** MediaScannerConnection. */
    private MediaScannerConnection mConnection = null;
    /** Zero value. */
    private static final int ZERO = 0;
    /** Files waiting for scanning. */
    private File[] mFiles;
    private Context mContext;
    /**
     * Constructor.
     *
     * @param context context
     */
    public MediaScannerUtil(Context context) {
        mContext = context;
        mConnection = new MediaScannerConnection(context.getApplicationContext(), this);
    }

    /**
     * Scan single a mix of mFiles&folders.
     *
     * @param files file array
     */
    public void scanFiles(File[] files) {
        this.mFiles = files;
        mConnection.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        if (null == mFiles || mFiles.length == ZERO) return;
        for (File file : mFiles) {
            scanSingleFile(file);
        }
        Toast.makeText(mContext,"扫描完成",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {

    }

    /**
     * Scan a single file/folder.
     *
     * @param file file
     */
    private void scanSingleFile(File file) {
        mConnection.scanFile(file.getPath(), null);
        if (file.exists() && file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (null != subFiles) {
                for (File subFile : subFiles) {
                    scanSingleFile(subFile);
                }
            }
        }
    }

    /**
     * Provide a method to disconnect Media to avoid memory leaks.
     */
    public void disconnect() {
        if (null != mConnection && mConnection.isConnected()) {
            mConnection.disconnect();
            mConnection = null;
        }
    }
}
