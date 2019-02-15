package io.mrarm.irc.build;

import com.squareup.javawriter.JavaWriter;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

@SuppressWarnings("unchecked")
public class SettingsBuilder {

    public static void generateJavaFiles(File sourceFile, File outputDir) throws IOException {
        Yaml yaml = new Yaml(new SafeConstructor());
        Map<?, ?> srcFile = yaml.load(new FileReader(sourceFile));
        String pkg = (String) srcFile.getOrDefault("package", null);
        for (Map.Entry e : srcFile.entrySet()) {
            if (e.getKey().equals("package"))
                continue;
            String filePath = pkg.replace('.', '/') + "/" + e.getKey() + ".java";
            generateJavaFile((Map) e.getValue(), pkg, (String) e.getKey(),
                    new File(outputDir, filePath));
        }
    }

    private static void generateJavaFile(Map map, String packageName, String className,
                                         File outputFile) throws IOException {
        outputFile.getParentFile().mkdirs();

        String prefPrefix = (String) map.getOrDefault("pref_prefix", null);
        List<String> imports = (List<String>) map.get("import");
        List<Map> settings = (List<Map>) map.get("settings");

        JavaWriter writer = new JavaWriter(new FileWriter(outputFile));
        writer.emitPackage(packageName);
        if (imports != null) {
            for (String inc : imports)
                writer.emitImports(inc);
        }

        writer.beginType(packageName + "." + className, "class",
                EnumSet.of(Modifier.PUBLIC, Modifier.FINAL));

        writer.beginMethod("android.content.SharedPreferences", "getPreferences",
                EnumSet.of(Modifier.PRIVATE, Modifier.STATIC));
        writer.emitStatement("return SettingsHelper.getPreferences()");
        writer.endMethod();
        writer.beginMethod("android.content.Context", "getContext",
                EnumSet.of(Modifier.PRIVATE, Modifier.STATIC));
        writer.emitStatement("return SettingsHelper.getContext()");
        writer.endMethod();

        for (Map s : settings) {
            String spec = (String) s.get("spec");
            int typeIof = spec.indexOf(' ');
            int setIof = spec.indexOf('=', typeIof + 1);
            String type = spec.substring(0, typeIof).trim();
            String name = (setIof != -1 ? spec.substring(typeIof + 1, setIof)
                    : spec.substring(typeIof + 1)).trim();
            String defaultValue =  setIof != -1 ? spec.substring(setIof + 1).trim() : null;
            String prefName = (String) s.get("pref");
            if (prefName == null) {
                prefName = camelToPascal(name);
                if (prefPrefix != null)
                    prefName = prefPrefix + "_" + prefName;
            }
            writeGetterFunc(writer, type, name, prefName, defaultValue, s);
        }
        writer.endType();
        writer.close();
    }

    private static void writeGetterFunc(JavaWriter writer, String type, String name,
                                        String prefName, String defValue, Map properties)
            throws IOException {
        String getterName = (String) properties.get("getter");
        if (getterName == null)
            getterName = getGetterPrefix(type) + capitalize(name);

        writer.beginMethod(type, getterName, EnumSet.of(Modifier.PUBLIC, Modifier.STATIC));

        switch (type) {
            case "boolean":
                writeSimpleGetterFuncBody(writer, "getBoolean", prefName, defValue);
                break;
            case "int":
                writeSimpleGetterFuncBody(writer, "getInt", prefName, defValue);
                break;
            case "String":
                writeSimpleGetterFuncBody(writer, "getString", prefName, defValue);
                break;
            case "String[]":
                writeStringArrGetterFuncBody(writer, prefName, defValue, properties);
                break;
            case "long":
                writer.emitStatement(
                        "return SettingsHelper.getLong(getPreferences(), \"%s\", %s)",
                        prefName, defValue);
                break;
            default:
                throw new IllegalArgumentException("Type not supported: " + type);
        }

        writer.endMethod();
    }

    private static void writeSimpleGetterFuncBody(JavaWriter writer, String type, String prefName,
                                                  String defValue) throws IOException {
        writer.emitStatement("return getPreferences().%s(\"%s\", %s)",
                type, prefName, defValue);
    }

    private static void writeStringArrGetterFuncBody(JavaWriter writer, String prefName,
                                                     String defValue, Map properties)
            throws IOException {
        String separator = (String) properties.get("separator");
        writer.emitStatement("String s = getPreferences().getString(\"%s\", null)",
                prefName);
        writer.beginControlFlow("if (s == null || s.length() == 0)");
        writer.emitStatement("return %s", defValue);
        writer.endControlFlow();
        writer.emitStatement("return s.split(String.valueOf(%s))", separator);
    }

    private static String getGetterPrefix(String type) {
        if (type.equals("boolean"))
            return "is";
        return "get";
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String camelToPascal(String str) {
        StringBuilder ret = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isUpperCase(c)) {
                ret.append('_');
                ret.append(Character.toLowerCase(c));
                continue;
            }
            ret.append(c);
        }
        return ret.toString();
    }

}