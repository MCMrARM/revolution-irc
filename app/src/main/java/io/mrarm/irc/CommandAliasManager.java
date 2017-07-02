package io.mrarm.irc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        sDefaultAliases.add(new CommandAlias("raw", "${args}"));
        sDefaultAliases.add(new CommandAlias("join", "JOIN :${args}"));
        sDefaultAliases.add(new CommandAlias("msg", "PRIVMSG ${args[0]} :${args[1:]}"));
        sDefaultAliases.add(new CommandAlias("me", "PRIVMSG ${channel} :${ctcp_delim}ACTION ${args}${ctcp_delim}"));
    }

    public static CommandAliasManager getInstance() {
        if (sInstance == null)
            sInstance = new CommandAliasManager();
        return sInstance;
    }

    public String processAlias(CommandAlias alias, SimpleTextVariableList variables) {
        Matcher matcher = mMatchVariablesRegex.matcher(alias.text);
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

    public String processCommand(String command, SimpleTextVariableList vars) {
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
            return null;
        String[] argsVar = new String[args.length - 1];
        System.arraycopy(args, 1, argsVar, 0, argsVar.length);
        vars.set(VAR_ARGS, Arrays.asList(argsVar), " ");
        return processAlias(alias, vars);
    }


    public static class CommandAlias {

        public String name;
        public String text;

        public CommandAlias() {
        }

        public CommandAlias(String name, String text) {
            this.name = name;
            this.text = text;
        }

    }

}
