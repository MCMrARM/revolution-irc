package io.mrarm.irc;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.irc.util.SettingsHelper;
import io.mrarm.irc.util.SimpleTextVariableList;

public class CommandAliasManager {

    static Pattern mMatchVariablesRegex = Pattern.compile("(?<!\\\\)\\$\\{(.*?)\\}");

    public static final String ALIASES_PATH = "command_aliases.json";

    public static final String VAR_CTCP_DELIM = "ctcp_delim";
    public static final String VAR_CTCP_DELIM_VALUE = "\001";

    public static final String VAR_CHANNEL = "channel";
    public static final String VAR_MYNICK = "nick";
    public static final String VAR_ARGS = "args";

    private static final SimpleTextVariableList sDefaultVariables;
    private static final List<CommandAlias> sDefaultAliases;

    private static CommandAliasManager sInstance;

    private Context mContext;
    private List<CommandAlias> mUserAliases;

    static {
        sDefaultVariables = new SimpleTextVariableList();
        sDefaultVariables.set(VAR_CTCP_DELIM, VAR_CTCP_DELIM_VALUE);

        sDefaultAliases = new ArrayList<>();
        sDefaultAliases.add(CommandAlias.raw("raw", "${args}"));
        sDefaultAliases.add(CommandAlias.raw("join", "JOIN :${args}"));
        sDefaultAliases.add(CommandAlias.message("msg", "${args[0]}", "${args[1:]}"));
        sDefaultAliases.add(CommandAlias.message("me", "${channel}", "${ctcp_delim}ACTION ${args}${ctcp_delim}"));
    }

    public static CommandAliasManager getInstance(Context context) {
        if (sInstance == null)
            sInstance = new CommandAliasManager(context.getApplicationContext());
        return sInstance;
    }

    public static List<CommandAlias> getDefaultAliases() {
        return sDefaultAliases;
    }

    public CommandAliasManager(Context context) {
        mContext = context;
        mUserAliases = new ArrayList<>();
        try {
            UserAliasesSettings settings = SettingsHelper.getGson().fromJson(new BufferedReader(
                            new FileReader(new File(context.getFilesDir(), ALIASES_PATH))),
                    UserAliasesSettings.class);
            mUserAliases = settings.userAliases;
        } catch (Exception ignored) {
        }
    }

    public boolean saveUserSettings() {
        try {
            UserAliasesSettings settings = new UserAliasesSettings();
            settings.userAliases = mUserAliases;
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    new File(mContext.getFilesDir(), ALIASES_PATH)));
            SettingsHelper.getGson().toJson(settings, writer);
            writer.close();
            return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    private static class UserAliasesSettings {

        List<CommandAlias> userAliases;

    }

    public List<CommandAlias> getUserAliases() {
        return mUserAliases;
    }

    public CommandAlias findCommandAlias(String name) {
        for (CommandAlias a : mUserAliases) {
            if (a.name.equalsIgnoreCase(name))
                return a;
        }
        for (CommandAlias a : sDefaultAliases) {
            if (a.name.equalsIgnoreCase(name))
                return a;
        }
        return null;
    }

    private String processVariables(String str, SimpleTextVariableList variables) {
        Matcher matcher = mMatchVariablesRegex.matcher(str);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            String text = matcher.group(1);
            String replaceWith = variables.get(text);
            if (replaceWith == null)
                replaceWith = sDefaultVariables.get(text);
            matcher.appendReplacement(buf, Matcher.quoteReplacement(replaceWith));
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    public void processCommand(IRCConnection connection, String command, SimpleTextVariableList vars) {
        String[] args = command.split(" ");
        CommandAlias alias = findCommandAlias(args[0]);
        if (alias == null)
            return;
        String[] argsVar = new String[args.length - 1];
        System.arraycopy(args, 1, argsVar, 0, argsVar.length);
        vars.set(VAR_ARGS, Arrays.asList(argsVar), " ");
        String processedText = processVariables(alias.text, vars);
        if (alias.mode == CommandAlias.MODE_RAW) {
            connection.sendCommandRaw(processedText, null, null);
        } else if (alias.mode == CommandAlias.MODE_MESSAGE) {
            String processedChannel = processVariables(alias.channel, vars);
            connection.sendMessage(processedChannel, processedText, null, null);
        }
    }


    public static class CommandAlias {

        public static final int MODE_RAW = 0;
        public static final int MODE_MESSAGE = 1;

        public String name;
        public String text;
        public int mode;
        public String channel;

        public CommandAlias() {
        }

        public static CommandAlias raw(String name, String text) {
            CommandAlias ret = new CommandAlias();
            ret.name = name;
            ret.text = text;
            ret.mode = MODE_RAW;
            return ret;
        }

        public static CommandAlias message(String name, String channel, String message) {
            CommandAlias ret = new CommandAlias();
            ret.name = name;
            ret.text = message;
            ret.channel = channel;
            ret.mode = MODE_MESSAGE;
            return ret;
        }

    }

}
