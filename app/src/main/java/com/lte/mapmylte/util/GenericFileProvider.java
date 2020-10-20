package com.lte.mapmylte.util;

import android.content.Context;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;

public class GenericFileProvider extends FileProvider {

    public static String createNewRecordCsvFileName()  {
        String mCsvFilename = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
        mCsvFilename = mCsvFilename.replace(' ', '_').replace(",", "");
        return String.format("%s.csv", mCsvFilename);

    }

    public static File getExternalDataFile(Context context, String fileName) {
        return new File(context.getExternalFilesDir(null), fileName);
    }
}


