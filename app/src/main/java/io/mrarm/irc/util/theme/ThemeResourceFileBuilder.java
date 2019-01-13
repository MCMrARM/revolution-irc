package io.mrarm.irc.util.theme;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.mrarm.arsc.ArscWriter;
import io.mrarm.arsc.chunks.ResTable;
import io.mrarm.arsc.chunks.ResValue;
import io.mrarm.irc.R;

public class ThemeResourceFileBuilder {

    private static int sessionThemeIndex = 0;

    private static ResTable createTheme(Context ctx) {
        ThemeHelper themeHelper = ThemeHelper.getInstance(ctx);

        ResTable table = new ResTable();
        ResTable.Package pkg = new ResTable.Package(0x7e, "io.mrarm.irc.theme");

        ResTable.TypeSpec colorTypeSpec = new ResTable.TypeSpec(1, "color", new int[] { 0, 0, 0 });
        pkg.addType(colorTypeSpec);
        ResTable.Type colorType = new ResTable.Type(1, new ResTable.Config());
        ResTable.Entry colorPrimary = new ResTable.Entry(0, "colorPrimary",
                new ResValue.Integer(ResValue.TYPE_INT_COLOR_ARGB8, themeHelper.getPrimaryColor()));
        colorType.addEntry(colorPrimary);
        ResTable.Entry colorPrimaryDark = new ResTable.Entry(1, "colorPrimaryDark",
                new ResValue.Integer(ResValue.TYPE_INT_COLOR_ARGB8, themeHelper.getPrimaryDarkColor()));
        colorType.addEntry(colorPrimaryDark);
        ResTable.Entry colorAccent = new ResTable.Entry(2, "colorAccent",
                new ResValue.Integer(ResValue.TYPE_INT_COLOR_ARGB8, themeHelper.getAccentColor()));
        colorType.addEntry(colorAccent);
        pkg.addType(colorType);

        ResTable.TypeSpec styleTypeSpec = new ResTable.TypeSpec(2, "style", new int[] { 0, 0 });
        pkg.addType(styleTypeSpec);
        ResTable.Type styleType = new ResTable.Type(2, new ResTable.Config());
        ResTable.MapEntry appTheme = new ResTable.MapEntry(0, "AppTheme");
        appTheme.setParent(R.style.AppTheme);
        appTheme.addValue(R.attr.colorPrimary, new ResValue.Reference(pkg, colorTypeSpec, colorPrimary));
        appTheme.addValue(R.attr.colorPrimaryDark, new ResValue.Reference(pkg, colorTypeSpec, colorPrimaryDark));
        appTheme.addValue(R.attr.colorAccent, new ResValue.Reference(pkg, colorTypeSpec, colorAccent));
        styleType.addEntry(appTheme);
        ResTable.MapEntry appThemeNoActionBar = new ResTable.MapEntry(1, "AppTheme");
        appThemeNoActionBar.setParent(R.style.AppTheme_NoActionBar);
        appThemeNoActionBar.addValue(R.attr.colorPrimary, new ResValue.Reference(pkg, colorTypeSpec, colorPrimary));
        appThemeNoActionBar.addValue(R.attr.colorPrimaryDark, new ResValue.Reference(pkg, colorTypeSpec, colorPrimaryDark));
        appThemeNoActionBar.addValue(R.attr.colorAccent, new ResValue.Reference(pkg, colorTypeSpec, colorAccent));
        styleType.addEntry(appThemeNoActionBar);
        pkg.addType(styleType);

        if (themeHelper.shouldUseLightToolbar()) {
            appTheme.setParent(R.style.AppTheme_CustomLightActionBar);
            appThemeNoActionBar.setParent(R.style.AppTheme_NoActionBar_CustomLightActionBar);
        }

        table.addPackage(pkg);
        return table;
    }

    public static int getPrimaryThemeId() {
        return ResTable.makeReference(0x7e, 2, 0);
    }

    public static int getNoActionBarThemeId() {
        return ResTable.makeReference(0x7e, 2, 1);
    }

    private static void buildThemeZipFile(File zipPath, ResTable resTable) {
        try {
            FileOutputStream fos = new FileOutputStream(zipPath);
            ZipOutputStream outStream = new ZipOutputStream(new BufferedOutputStream(fos));

            ZipEntry entry = new ZipEntry("resources.arsc");
            outStream.putNextEntry(entry);

            ArscWriter writer = new ArscWriter(resTable);
            writer.write(outStream);

            outStream.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File getThemesDir(Context context) {
        return new File(context.getFilesDir(), "themes");
    }

    private static void deleteOldThemeFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null)
            return;
        for (File file : files)
            file.delete();
    }

    public static File createThemeZipFile(Context context) {
        File dir = getThemesDir(context);
        dir.mkdirs();
        deleteOldThemeFiles(dir);
        File file = new File(dir, "theme." + sessionThemeIndex +  ".zip");
        buildThemeZipFile(file, createTheme(context));
        ++sessionThemeIndex;
        return file;
    }

}
