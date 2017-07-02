package io.mrarm.irc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.irc.util.SimpleTextVariableList;

public class CommandAliasManager {

    static Pattern mMatchVariablesRegex = Pattern.compile("(?<!\\\\)\\$\\{(.*?)\\}");

    public static final String VAR_CTCP_DELIM = "ctcp_delim";
    public static final String VAR_CTCP_DELIM_VALUE = "\001";

    public static final String VAR_CHANNEL = "channel";
    public static final String VAR_MYNICK = "nick";
    public static final String VAR_ARGS = "args";

    private static final SimpleTextVariableList sDefaultVariables;
    private static final List<CommandAlias> sDefaultAliases;

    private static CommandAliasManager sInstance;

    static {
        sDefaultVariables = new SimpleTextVariableList();
        sDefaultVariables.set(VAR_CTCP_DELIM, VAR_CTCP_DELIM_VALUE);

        sDefaultAliases = new ArrayList<>();
        sDefaultAliases.add(CommandAlias.raw("raw", "${args}"));
        sDefaultAliases.add(CommandAlias.raw("join", "JOIN :${args}"));
        sDefaultAliases.add(CommandAlias.message("msg", "${args[0]}", "${args[1:]}"));
        sDefaultAliases.add(CommandAlias.message("me", "${channel}", "${ctcp_delim}ACTION ${args}${ctcp_delim}"));
    }

    public static CommandAliasManager getInstance() {
        if (sInstance == null)
            sInstance = new CommandAliasManager();
        return sInstance;
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
        String name = args[0];
        CommandAlias alias = null;
        for (CommandAlias a : sDefaultAliases) {
            if (a.name.equalsIgnoreCase(name)) {
                alias = a;
                break;
            }
        }
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
