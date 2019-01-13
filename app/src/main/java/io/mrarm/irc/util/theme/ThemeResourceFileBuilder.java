package io.mrarm.irc.util.theme;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.mrarm.arsc.ArscWriter;
import io.mrarm.arsc.chunks.ResTable;
import io.mrarm.arsc.chunks.ResValue;
import io.mrarm.irc.R;

public class ThemeResourceFileBuilder {

    private static int sessionThemeIndex = 0;

    private static final Map<String, List<Integer>> colorToAttrs = new HashMap<>();

    private static void mapColorToAttr(String colorName, int attr) {
        List<Integer> l = colorToAttrs.get(colorName);
        if (l == null) {
            l = new ArrayList<>();
            colorToAttrs.put(colorName, l);
        }
        l.add(attr);
    }

    static {
        mapColorToAttr(ThemeInfo.COLOR_PRIMARY, R.attr.colorPrimary);
        mapColorToAttr(ThemeInfo.COLOR_PRIMARY_DARK, R.attr.colorPrimaryDark);
        mapColorToAttr(ThemeInfo.COLOR_ACCENT, R.attr.colorAccent);
    }

    private static void setUseLightActionBar(ResTable.MapEntry theme) {
        theme.addValue(R.attr.actionBarPopupTheme,
                new ResValue.Reference(R.style.ThemeOverlay_AppCompat));
        theme.addValue(R.attr.actionBarTheme,
                new ResValue.Reference(R.style.ThemeOverlay_AppCompat_ActionBar));
    }

    public static CustomTheme createTheme(Context ctx, ThemeInfo theme,
                                        ThemeManager.ThemeResInfo baseTheme) {
        ResTable table = new ResTable();
        ResTable.Package pkg = new ResTable.Package(0x7e, "io.mrarm.irc.theme");

        ResTable.TypeSpec colorTypeSpec = new ResTable.TypeSpec(1, "color",
                new int[theme.colors.size()] /* should be filled with zeros */);
        pkg.addType(colorTypeSpec);
        ResTable.Type colorType = new ResTable.Type(1, new ResTable.Config());
        List<String> colors = new ArrayList<>();
        for (Map.Entry<String, Integer> p : theme.colors.entrySet()) {
            ResTable.Entry colorPrimary = new ResTable.Entry(colors.size(), p.getKey(),
                    new ResValue.Integer(ResValue.TYPE_INT_COLOR_ARGB8, p.getValue()));
            colorType.addEntry(colorPrimary);
            colors.add(p.getKey());
        }
        pkg.addType(colorType);

        ResTable.TypeSpec styleTypeSpec = new ResTable.TypeSpec(2, "style", new int[] { 0, 0 });
        pkg.addType(styleTypeSpec);
        ResTable.Type styleType = new ResTable.Type(2, new ResTable.Config());
        ResTable.MapEntry appTheme = new ResTable.MapEntry(0, "AppTheme");
        appTheme.setParent(baseTheme.getThemeResId());
        styleType.addEntry(appTheme);
        ResTable.MapEntry appThemeNoActionBar = new ResTable.MapEntry(1, "AppTheme.NoActionBar");
        appThemeNoActionBar.setParent(baseTheme.getThemeNoActionBarResId());
        styleType.addEntry(appThemeNoActionBar);
        pkg.addType(styleType);

        for (int i = 0; i < colors.size(); i++) {
            String color = colors.get(i);
            List<Integer> resIds = colorToAttrs.get(color);
            if (resIds == null)
                continue;
            for (int resId : resIds)
                appTheme.addValue(resId, new ResValue.Reference(pkg, colorTypeSpec, i));
            for (int resId : resIds)
                appThemeNoActionBar.addValue(resId, new ResValue.Reference(pkg, colorTypeSpec, i));
        }
        if (theme.lightToolbar) {
            setUseLightActionBar(appTheme);
            setUseLightActionBar(appThemeNoActionBar);
        }

        table.addPackage(pkg);
        return new CustomTheme(ResTable.makeReference(pkg, styleTypeSpec, appTheme),
                ResTable.makeReference(pkg, styleTypeSpec, appThemeNoActionBar), table);
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

    public static File createThemeZipFile(Context context, ResTable resTable) {
        File dir = getThemesDir(context);
        dir.mkdirs();
        deleteOldThemeFiles(dir);
        File file = new File(dir, "theme." + sessionThemeIndex +  ".zip");
        buildThemeZipFile(file, resTable);
        ++sessionThemeIndex;
        return file;
    }


    public static class CustomTheme extends ThemeManager.ThemeResInfo {

        private ResTable resTable;

        public CustomTheme(int themeResId, int themeNoActionBarResId, ResTable resTable) {
            super(themeResId, themeNoActionBarResId);
            this.resTable = resTable;
        }

        public ResTable getResTable() {
            return resTable;
        }

    }

}
