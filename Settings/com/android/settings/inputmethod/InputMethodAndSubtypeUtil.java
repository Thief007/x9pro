package com.android.settings.inputmethod;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.settings.SettingsPreferenceFragment;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

class InputMethodAndSubtypeUtil {
    private static final SimpleStringSplitter sStringInputMethodSplitter = new SimpleStringSplitter(':');
    private static final SimpleStringSplitter sStringInputMethodSubtypeSplitter = new SimpleStringSplitter(';');

    InputMethodAndSubtypeUtil() {
    }

    static String buildInputMethodsAndSubtypesString(HashMap<String, HashSet<String>> imeToSubtypesMap) {
        StringBuilder builder = new StringBuilder();
        for (String imi : imeToSubtypesMap.keySet()) {
            if (builder.length() > 0) {
                builder.append(':');
            }
            HashSet<String> subtypeIdSet = (HashSet) imeToSubtypesMap.get(imi);
            builder.append(imi);
            for (String subtypeId : subtypeIdSet) {
                builder.append(';').append(subtypeId);
            }
        }
        return builder.toString();
    }

    private static String buildInputMethodsString(HashSet<String> imiList) {
        StringBuilder builder = new StringBuilder();
        for (String imi : imiList) {
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(imi);
        }
        return builder.toString();
    }

    private static int getInputMethodSubtypeSelected(ContentResolver resolver) {
        try {
            return Secure.getInt(resolver, "selected_input_method_subtype");
        } catch (SettingNotFoundException e) {
            return -1;
        }
    }

    private static boolean isInputMethodSubtypeSelected(ContentResolver resolver) {
        return getInputMethodSubtypeSelected(resolver) != -1;
    }

    private static void putSelectedInputMethodSubtype(ContentResolver resolver, int hashCode) {
        Secure.putInt(resolver, "selected_input_method_subtype", hashCode);
    }

    private static HashMap<String, HashSet<String>> getEnabledInputMethodsAndSubtypeList(ContentResolver resolver) {
        return parseInputMethodsAndSubtypesString(Secure.getString(resolver, "enabled_input_methods"));
    }

    static HashMap<String, HashSet<String>> parseInputMethodsAndSubtypesString(String inputMethodsAndSubtypesString) {
        HashMap<String, HashSet<String>> subtypesMap = new HashMap();
        if (TextUtils.isEmpty(inputMethodsAndSubtypesString)) {
            return subtypesMap;
        }
        sStringInputMethodSplitter.setString(inputMethodsAndSubtypesString);
        while (sStringInputMethodSplitter.hasNext()) {
            sStringInputMethodSubtypeSplitter.setString(sStringInputMethodSplitter.next());
            if (sStringInputMethodSubtypeSplitter.hasNext()) {
                HashSet<String> subtypeIdSet = new HashSet();
                String imiId = sStringInputMethodSubtypeSplitter.next();
                while (sStringInputMethodSubtypeSplitter.hasNext()) {
                    subtypeIdSet.add(sStringInputMethodSubtypeSplitter.next());
                }
                subtypesMap.put(imiId, subtypeIdSet);
            }
        }
        return subtypesMap;
    }

    static void enableInputMethodSubtypesOf(ContentResolver resolver, String imiId, HashSet<String> enabledSubtypeIdSet) {
        HashMap<String, HashSet<String>> enabledImeAndSubtypeIdsMap = getEnabledInputMethodsAndSubtypeList(resolver);
        enabledImeAndSubtypeIdsMap.put(imiId, enabledSubtypeIdSet);
        Secure.putString(resolver, "enabled_input_methods", buildInputMethodsAndSubtypesString(enabledImeAndSubtypeIdsMap));
    }

    private static HashSet<String> getDisabledSystemIMEs(ContentResolver resolver) {
        HashSet<String> set = new HashSet();
        String disabledIMEsStr = Secure.getString(resolver, "disabled_system_input_methods");
        if (TextUtils.isEmpty(disabledIMEsStr)) {
            return set;
        }
        sStringInputMethodSplitter.setString(disabledIMEsStr);
        while (sStringInputMethodSplitter.hasNext()) {
            set.add(sStringInputMethodSplitter.next());
        }
        return set;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void saveInputMethodSubtypeList(SettingsPreferenceFragment context, ContentResolver resolver, List<InputMethodInfo> inputMethodInfos, boolean hasHardKeyboard) {
        String currentInputMethodId = Secure.getString(resolver, "default_input_method");
        int selectedInputMethodSubtype = getInputMethodSubtypeSelected(resolver);
        HashMap<String, HashSet<String>> enabledIMEsAndSubtypesMap = getEnabledInputMethodsAndSubtypeList(resolver);
        HashSet<String> disabledSystemIMEs = getDisabledSystemIMEs(resolver);
        boolean needsToResetSelectedSubtype = false;
        for (InputMethodInfo imi : inputMethodInfos) {
            String imiId = imi.getId();
            Preference pref = context.findPreference(imiId);
            if (pref != null) {
                boolean isImeChecked;
                if (pref instanceof TwoStatePreference) {
                    isImeChecked = ((TwoStatePreference) pref).isChecked();
                } else {
                    isImeChecked = enabledIMEsAndSubtypesMap.containsKey(imiId);
                }
                boolean isCurrentInputMethod = imiId.equals(currentInputMethodId);
                boolean systemIme = InputMethodUtils.isSystemIme(imi);
                if (!hasHardKeyboard) {
                }
                if (!isImeChecked) {
                    enabledIMEsAndSubtypesMap.remove(imiId);
                    if (isCurrentInputMethod) {
                        currentInputMethodId = null;
                    }
                    if (systemIme && hasHardKeyboard) {
                        if (disabledSystemIMEs.contains(imiId)) {
                            if (!isImeChecked) {
                                disabledSystemIMEs.add(imiId);
                            }
                        } else if (isImeChecked) {
                            disabledSystemIMEs.remove(imiId);
                        }
                    }
                }
                if (!enabledIMEsAndSubtypesMap.containsKey(imiId)) {
                    enabledIMEsAndSubtypesMap.put(imiId, new HashSet());
                }
                HashSet<String> subtypesSet = (HashSet) enabledIMEsAndSubtypesMap.get(imiId);
                boolean subtypePrefFound = false;
                int subtypeCount = imi.getSubtypeCount();
                for (int i = 0; i < subtypeCount; i++) {
                    InputMethodSubtype subtype = imi.getSubtypeAt(i);
                    String subtypeHashCodeStr = String.valueOf(subtype.hashCode());
                    TwoStatePreference subtypePref = (TwoStatePreference) context.findPreference(imiId + subtypeHashCodeStr);
                    if (subtypePref != null) {
                        if (!subtypePrefFound) {
                            subtypesSet.clear();
                            needsToResetSelectedSubtype = true;
                            subtypePrefFound = true;
                        }
                        if (subtypePref.isChecked()) {
                            subtypesSet.add(subtypeHashCodeStr);
                            if (isCurrentInputMethod && selectedInputMethodSubtype == subtype.hashCode()) {
                                needsToResetSelectedSubtype = false;
                            }
                        } else {
                            subtypesSet.remove(subtypeHashCodeStr);
                        }
                    }
                }
                if (disabledSystemIMEs.contains(imiId)) {
                    if (!isImeChecked) {
                        disabledSystemIMEs.add(imiId);
                    }
                } else if (isImeChecked) {
                    disabledSystemIMEs.remove(imiId);
                }
            }
        }
        String enabledIMEsAndSubtypesString = buildInputMethodsAndSubtypesString(enabledIMEsAndSubtypesMap);
        String disabledSystemIMEsString = buildInputMethodsString(disabledSystemIMEs);
        if (needsToResetSelectedSubtype || !isInputMethodSubtypeSelected(resolver)) {
            putSelectedInputMethodSubtype(resolver, -1);
        }
        Secure.putString(resolver, "enabled_input_methods", enabledIMEsAndSubtypesString);
        if (disabledSystemIMEsString.length() > 0) {
            Secure.putString(resolver, "disabled_system_input_methods", disabledSystemIMEsString);
        }
        String str = "default_input_method";
        if (currentInputMethodId == null) {
            currentInputMethodId = "";
        }
        Secure.putString(resolver, str, currentInputMethodId);
    }

    static void loadInputMethodSubtypeList(SettingsPreferenceFragment context, ContentResolver resolver, List<InputMethodInfo> inputMethodInfos, Map<String, List<Preference>> inputMethodPrefsMap) {
        HashMap<String, HashSet<String>> enabledSubtypes = getEnabledInputMethodsAndSubtypeList(resolver);
        for (InputMethodInfo imi : inputMethodInfos) {
            String imiId = imi.getId();
            Preference pref = context.findPreference(imiId);
            if (pref instanceof TwoStatePreference) {
                TwoStatePreference subtypePref = (TwoStatePreference) pref;
                boolean isEnabled = enabledSubtypes.containsKey(imiId);
                subtypePref.setChecked(isEnabled);
                if (inputMethodPrefsMap != null) {
                    for (Preference childPref : (List) inputMethodPrefsMap.get(imiId)) {
                        childPref.setEnabled(isEnabled);
                    }
                }
                setSubtypesPreferenceEnabled(context, inputMethodInfos, imiId, isEnabled);
            }
        }
        updateSubtypesPreferenceChecked(context, inputMethodInfos, enabledSubtypes);
    }

    static void setSubtypesPreferenceEnabled(SettingsPreferenceFragment context, List<InputMethodInfo> inputMethodProperties, String id, boolean enabled) {
        PreferenceScreen preferenceScreen = context.getPreferenceScreen();
        for (InputMethodInfo imi : inputMethodProperties) {
            if (id.equals(imi.getId())) {
                int subtypeCount = imi.getSubtypeCount();
                for (int i = 0; i < subtypeCount; i++) {
                    TwoStatePreference pref = (TwoStatePreference) preferenceScreen.findPreference(id + imi.getSubtypeAt(i).hashCode());
                    if (pref != null) {
                        pref.setEnabled(enabled);
                    }
                }
            }
        }
    }

    private static void updateSubtypesPreferenceChecked(SettingsPreferenceFragment context, List<InputMethodInfo> inputMethodProperties, HashMap<String, HashSet<String>> enabledSubtypes) {
        PreferenceScreen preferenceScreen = context.getPreferenceScreen();
        for (InputMethodInfo imi : inputMethodProperties) {
            String id = imi.getId();
            if (enabledSubtypes.containsKey(id)) {
                HashSet<String> enabledSubtypesSet = (HashSet) enabledSubtypes.get(id);
                int subtypeCount = imi.getSubtypeCount();
                for (int i = 0; i < subtypeCount; i++) {
                    String hashCode = String.valueOf(imi.getSubtypeAt(i).hashCode());
                    TwoStatePreference pref = (TwoStatePreference) preferenceScreen.findPreference(id + hashCode);
                    if (pref != null) {
                        pref.setChecked(enabledSubtypesSet.contains(hashCode));
                    }
                }
            }
        }
    }

    static void removeUnnecessaryNonPersistentPreference(Preference pref) {
        String key = pref.getKey();
        if (!pref.isPersistent() && key != null) {
            SharedPreferences prefs = pref.getSharedPreferences();
            if (prefs != null && prefs.contains(key)) {
                prefs.edit().remove(key).apply();
            }
        }
    }
}
