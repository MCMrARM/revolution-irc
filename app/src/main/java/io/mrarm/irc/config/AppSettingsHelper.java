package io.mrarm.irc.config;

import java.util.List;

import io.mrarm.irc.setting.ReconnectIntervalSetting;

class AppSettingsHelper {

    private static List<ReconnectIntervalSetting.Rule> sCachedIntervalRules;

    static List<ReconnectIntervalSetting.Rule> getReconnectIntervalRules() {
        if (sCachedIntervalRules == null) {
            sCachedIntervalRules = ReconnectIntervalSetting.getDefaultValue();
            try {
                List<ReconnectIntervalSetting.Rule> rules = SettingsHelper.getGson().fromJson(
                        AppSettings.getReconnectIntervalString(), ReconnectIntervalSetting.sListRuleType);
                if (rules != null)
                    sCachedIntervalRules = rules;
            } catch (Exception ignored) {
            }
        }
        return sCachedIntervalRules;
    }

    static {
        SettingsHelper.changeEvent().listen(AppSettings.PREF_RECONNECT_INTERVAL, () -> {
            sCachedIntervalRules = null;
        });
    }

}
