package io.mrarm.irc.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleTextVariableList {

    private Map<String, String> mVars = new HashMap<>();
    private Map<String, List<String>> mArrVars = new HashMap<>();
    private Map<String, String> mArrVarJoinStr = new HashMap<>();

    public SimpleTextVariableList() {
    }

    public SimpleTextVariableList(SimpleTextVariableList copy) {
        mVars.putAll(copy.mVars);
        mArrVars.putAll(copy.mArrVars);
        mArrVarJoinStr.putAll(copy.mArrVarJoinStr);
    }

    public void set(String name, String value) {
        mVars.put(name, value);
    }

    public void set(String name, List<String> values, String arrJoinStr) {
        mArrVars.put(name, values);
        mArrVarJoinStr.put(name, arrJoinStr);
    }

    public String get(String text) {
        int iof = text.endsWith("]") ? text.indexOf('[') : -1;
        if (mVars.containsKey(text))
            return mVars.get(text);
        String arrName = (iof != -1 ? text.substring(0, iof) : text);
        List<String> arr = mArrVars.get(arrName);
        if (arr == null)
            return null;
        String joinStr = mArrVarJoinStr.get(arrName);
        String params = iof != -1 ? text.substring(iof + 1, text.length() - 1) : null;
        int startI = 0, endI = arr.size() - 1;
        if (params != null && params.length() > 0) {
            iof = params.indexOf(':');
            startI = iof == 0 ? 0 : Integer.parseInt(iof == -1 ? params : params.substring(0, iof));
            endI = iof == -1 ? startI : (iof == params.length() -1 ? endI : Integer.parseInt(params.substring(iof + 1)));
            if (startI < 0)
                startI = Math.max(arr.size() - startI, 0);
            if (endI < 0)
                endI = Math.max(arr.size() - endI, 0);
        }
        StringBuilder ret = new StringBuilder();
        for (int i = startI; i <= endI; i++) {
            if (i != startI)
                ret.append(joinStr);
            ret.append(arr.get(i));
        }
        return ret.toString();
    }

}
