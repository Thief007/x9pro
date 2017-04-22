package com.android.settingslib.datetime;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.R$xml;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import libcore.icu.TimeZoneNames;
import org.xmlpull.v1.XmlPullParserException;

public class ZoneGetter {
    private ZoneGetter() {
    }

    public static String getTimeZoneOffsetAndName(TimeZone tz, Date now) {
        Locale locale = Locale.getDefault();
        String gmtString = getGmtOffsetString(locale, tz, now);
        String zoneNameString = getZoneLongName(locale, tz, now);
        if (zoneNameString == null) {
            return gmtString;
        }
        return gmtString + " " + zoneNameString;
    }

    public static List<Map<String, Object>> getZonesList(Context context) {
        Locale locale = Locale.getDefault();
        Date now = new Date();
        List<String> olsonIdsToDisplay = readTimezonesToDisplay(context);
        Set<String> localZoneIds = new TreeSet();
        for (String olsonId : TimeZoneNames.forLocale(locale)) {
            localZoneIds.add(olsonId);
        }
        Set<String> localZoneNames = new TreeSet();
        boolean localLongNamesAreAmbiguous = false;
        for (String olsonId2 : olsonIdsToDisplay) {
            if (localZoneIds.contains(olsonId2) && !localZoneNames.add(getZoneLongName(locale, TimeZone.getTimeZone(olsonId2), now))) {
                localLongNamesAreAmbiguous = true;
                break;
            }
        }
        List<Map<String, Object>> zones = new ArrayList();
        for (String olsonId22 : olsonIdsToDisplay) {
            TimeZone tz = TimeZone.getTimeZone(olsonId22);
            boolean preferLongName = localZoneIds.contains(olsonId22) && !localLongNamesAreAmbiguous;
            zones.add(createDisplayEntry(tz, getGmtOffsetString(locale, tz, now), getZoneDisplayName(locale, tz, now, preferLongName), tz.getOffset(now.getTime())));
        }
        return zones;
    }

    private static Map<String, Object> createDisplayEntry(TimeZone tz, String gmtOffsetString, String displayName, int offsetMillis) {
        Map<String, Object> map = new HashMap();
        map.put("id", tz.getID());
        map.put("name", displayName);
        map.put("gmt", gmtOffsetString);
        map.put("offset", Integer.valueOf(offsetMillis));
        return map;
    }

    private static String getZoneDisplayName(Locale locale, TimeZone tz, Date now, boolean preferLongName) {
        if (preferLongName) {
            return getZoneLongName(locale, tz, now);
        }
        String zoneNameString = getZoneExemplarLocation(locale, tz);
        if (zoneNameString == null || zoneNameString.isEmpty()) {
            return getZoneLongName(locale, tz, now);
        }
        return zoneNameString;
    }

    private static String getZoneExemplarLocation(Locale locale, TimeZone tz) {
        return TimeZoneNames.getExemplarLocation(locale.toString(), tz.getID());
    }

    private static List<String> readTimezonesToDisplay(Context context) {
        Throwable th;
        Throwable th2 = null;
        List<String> olsonIds = new ArrayList();
        XmlResourceParser xmlResourceParser = null;
        xmlResourceParser = context.getResources().getXml(R$xml.timezones);
        do {
        } while (xmlResourceParser.next() != 2);
        xmlResourceParser.next();
        while (xmlResourceParser.getEventType() != 3) {
            while (xmlResourceParser.getEventType() != 2) {
                if (xmlResourceParser.getEventType() == 1) {
                    if (xmlResourceParser != null) {
                        try {
                            xmlResourceParser.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 == null) {
                        return olsonIds;
                    }
                    try {
                        throw th2;
                    } catch (XmlPullParserException e) {
                        Log.e("ZoneGetter", "Ill-formatted timezones.xml file");
                    } catch (IOException e2) {
                        Log.e("ZoneGetter", "Unable to read timezones.xml file");
                    }
                } else {
                    xmlResourceParser.next();
                }
            }
            try {
                if (xmlResourceParser.getName().equals("timezone")) {
                    olsonIds.add(xmlResourceParser.getAttributeValue(0));
                }
                while (xmlResourceParser.getEventType() != 3) {
                    xmlResourceParser.next();
                }
                xmlResourceParser.next();
            } catch (Throwable th22) {
                Throwable th4 = th22;
                th22 = th;
                th = th4;
            }
        }
        if (xmlResourceParser != null) {
            try {
                xmlResourceParser.close();
            } catch (Throwable th5) {
                th22 = th5;
            }
        }
        if (th22 != null) {
            throw th22;
        }
        return olsonIds;
        if (xmlResourceParser != null) {
            try {
                xmlResourceParser.close();
            } catch (Throwable th6) {
                if (th22 == null) {
                    th22 = th6;
                } else if (th22 != th6) {
                    th22.addSuppressed(th6);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        } else {
            throw th;
        }
    }

    private static String getZoneLongName(Locale locale, TimeZone tz, Date now) {
        return tz.getDisplayName(tz.inDaylightTime(now), 1, locale);
    }

    private static String getGmtOffsetString(Locale locale, TimeZone tz, Date now) {
        SimpleDateFormat gmtFormatter = new SimpleDateFormat("ZZZZ");
        gmtFormatter.setTimeZone(tz);
        return BidiFormatter.getInstance().unicodeWrap(gmtFormatter.format(now), TextUtils.getLayoutDirectionFromLocale(locale) == 1 ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR);
    }
}
