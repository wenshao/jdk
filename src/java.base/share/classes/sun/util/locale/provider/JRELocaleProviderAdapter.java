/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.util.locale.provider;

import java.text.spi.BreakIteratorProvider;
import java.text.spi.CollatorProvider;
import java.text.spi.DateFormatProvider;
import java.text.spi.DateFormatSymbolsProvider;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.text.spi.NumberFormatProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.spi.CalendarDataProvider;
import java.util.spi.CalendarNameProvider;
import java.util.spi.CurrencyNameProvider;
import java.util.spi.LocaleNameProvider;
import java.util.spi.LocaleServiceProvider;
import java.util.spi.TimeZoneNameProvider;
import sun.text.spi.JavaTimeDateTimePatternProvider;
import sun.util.resources.LocaleData;
import sun.util.spi.CalendarProvider;

/**
 * LocaleProviderAdapter implementation for the legacy JRE locale data.
 *
 * @author Naoto Sato
 * @author Masayoshi Okutsu
 */
public class JRELocaleProviderAdapter extends LocaleProviderAdapter implements ResourceBundleBasedAdapter {

    private final ConcurrentMap<String, Set<String>> langtagSets
        = new ConcurrentHashMap<>();

    private final ConcurrentMap<Locale, LocaleResources> localeResourcesMap
        = new ConcurrentHashMap<>();

    // LocaleData specific to this LocaleProviderAdapter.
    private volatile LocaleData localeData;

    /**
     * Returns the type of this LocaleProviderAdapter
     */
    @Override
    public LocaleProviderAdapter.Type getAdapterType() {
        return Type.JRE;
    }

    /**
     * Getter method for Locale Service Providers
     */
    @Override
    @SuppressWarnings("unchecked")
    public <P extends LocaleServiceProvider> P getLocaleServiceProvider(Class<P> c) {
        switch (c.getSimpleName()) {
        case "BreakIteratorProvider":
            return (P) getBreakIteratorProvider();
        case "CollatorProvider":
            return (P) getCollatorProvider();
        case "DateFormatProvider":
            return (P) getDateFormatProvider();
        case "DateFormatSymbolsProvider":
            return (P) getDateFormatSymbolsProvider();
        case "DecimalFormatSymbolsProvider":
            return (P) getDecimalFormatSymbolsProvider();
        case "NumberFormatProvider":
            return (P) getNumberFormatProvider();
        case "CurrencyNameProvider":
            return (P) getCurrencyNameProvider();
        case "LocaleNameProvider":
            return (P) getLocaleNameProvider();
        case "TimeZoneNameProvider":
            return (P) getTimeZoneNameProvider();
        case "CalendarDataProvider":
            return (P) getCalendarDataProvider();
        case "CalendarNameProvider":
            return (P) getCalendarNameProvider();
        case "CalendarProvider":
            return (P) getCalendarProvider();
        case "JavaTimeDateTimePatternProvider":
            return (P) getJavaTimeDateTimePatternProvider();
        default:
            throw new InternalError("should not come down here");
        }
    }

    private final StableValue<BreakIteratorProvider> breakIteratorProvider = StableValue.of();
    private final StableValue<CollatorProvider> collatorProvider = StableValue.of();
    private final StableValue<DateFormatProvider> dateFormatProvider = StableValue.of();
    private final StableValue<DateFormatSymbolsProvider> dateFormatSymbolsProvider = StableValue.of();
    private final StableValue<DecimalFormatSymbolsProvider> decimalFormatSymbolsProvider = StableValue.of();
    private final StableValue<NumberFormatProvider> numberFormatProvider = StableValue.of();

    private final StableValue<CurrencyNameProvider> currencyNameProvider = StableValue.of();
    private final StableValue<LocaleNameProvider> localeNameProvider = StableValue.of();
    protected final StableValue<TimeZoneNameProvider> timeZoneNameProvider = StableValue.of();
    protected final StableValue<CalendarDataProvider> calendarDataProvider = StableValue.of();
    protected final StableValue<CalendarNameProvider> calendarNameProvider = StableValue.of();

    private final StableValue<CalendarProvider> calendarProvider = StableValue.of();
    private final StableValue<JavaTimeDateTimePatternProvider> javaTimeDateTimePatternProvider = StableValue.of();

    /*
     * Getter methods for java.text.spi.* providers
     */
    @Override
    public BreakIteratorProvider getBreakIteratorProvider() {
        return breakIteratorProvider.orElseSet(() -> new BreakIteratorProviderImpl(
                getAdapterType(),
                getLanguageTagSet("FormatData")));
    }

    @Override
    public CollatorProvider getCollatorProvider() {
        return collatorProvider.orElseSet(() -> new CollatorProviderImpl(
                getAdapterType(),
                getLanguageTagSet("CollationData")));
    }

    @Override
    public DateFormatProvider getDateFormatProvider() {
        return dateFormatProvider.orElseSet(() -> new DateFormatProviderImpl(
                getAdapterType(),
                getLanguageTagSet("FormatData")));
    }

    @Override
    public DateFormatSymbolsProvider getDateFormatSymbolsProvider() {
        return dateFormatSymbolsProvider.orElseSet(() -> new DateFormatSymbolsProviderImpl(
                getAdapterType(),
                getLanguageTagSet("FormatData")));
    }

    @Override
    public DecimalFormatSymbolsProvider getDecimalFormatSymbolsProvider() {
        return decimalFormatSymbolsProvider.orElseSet(() -> new DecimalFormatSymbolsProviderImpl(
                getAdapterType(),
                getLanguageTagSet("FormatData")));
    }

    @Override
    public NumberFormatProvider getNumberFormatProvider() {
        return numberFormatProvider.orElseSet(() -> new NumberFormatProviderImpl(
                getAdapterType(),
                getLanguageTagSet("FormatData")));
    }

    /**
     * Getter methods for java.util.spi.* providers
     */
    @Override
    public CurrencyNameProvider getCurrencyNameProvider() {
        return currencyNameProvider.orElseSet(() -> new CurrencyNameProviderImpl(
                getAdapterType(),
                getLanguageTagSet("CurrencyNames")));
    }

    @Override
    public LocaleNameProvider getLocaleNameProvider() {
        return localeNameProvider.orElseSet(() -> new LocaleNameProviderImpl(
                getAdapterType(),
                getLanguageTagSet("LocaleNames")));
    }

    @Override
    public TimeZoneNameProvider getTimeZoneNameProvider() {
        return timeZoneNameProvider.orElseSet(() -> new TimeZoneNameProviderImpl(
                getAdapterType(),
                getLanguageTagSet("TimeZoneNames")));
    }

    @Override
    public CalendarDataProvider getCalendarDataProvider() {
        return calendarDataProvider.orElseSet(() -> new CalendarDataProviderImpl(
                getAdapterType(),
                getLanguageTagSet("CalendarData")));
    }

    @Override
    public CalendarNameProvider getCalendarNameProvider() {
        return calendarNameProvider.orElseSet(() -> new CalendarNameProviderImpl(
                getAdapterType(),
                getLanguageTagSet("FormatData")));
    }

    /**
     * Getter methods for sun.util.spi.* providers
     */
    @Override
    public CalendarProvider getCalendarProvider() {
        return calendarProvider.orElseSet(() -> new CalendarProviderImpl(
                getAdapterType(),
                getLanguageTagSet("CalendarData")));
    }

    /**
     * Getter methods for sun.text.spi.JavaTimeDateTimePatternProvider provider
     */
    @Override
    public JavaTimeDateTimePatternProvider getJavaTimeDateTimePatternProvider() {
        return javaTimeDateTimePatternProvider.orElseSet(() -> new JavaTimeDateTimePatternImpl(
                getAdapterType(),
                getLanguageTagSet("FormatData")));
    }

    @Override
    public LocaleResources getLocaleResources(Locale locale) {
        LocaleResources lr = localeResourcesMap.get(locale);
        if (lr == null) {
            lr = new LocaleResources(this, locale);
            LocaleResources lrc = localeResourcesMap.putIfAbsent(locale, lr);
            if (lrc != null) {
                lr = lrc;
            }
        }
        return lr;
    }

    // ResourceBundleBasedAdapter method implementation

    @Override
    public LocaleData getLocaleData() {
        if (localeData == null) {
            synchronized (this) {
                if (localeData == null) {
                    localeData = new LocaleData(getAdapterType());
                }
            }
        }
        return localeData;
    }

    @Override
    public List<Locale> getCandidateLocales(String baseName, Locale locale) {
        return ResourceBundle.Control
            .getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)
            .getCandidateLocales(baseName, locale);
    }

    /**
     * Returns a list of the installed locales. Currently, this simply returns
     * the list of locales for which a sun.text.resources.FormatData bundle
     * exists. This bundle family happens to be the one with the broadest
     * locale coverage in the JRE.
     */
    @Override
    public Locale[] getAvailableLocales() {
        return AvailableJRELocales.localeList;
    }

    public Set<String> getLanguageTagSet(String category) {
        Set<String> tagset = langtagSets.get(category);
        if (tagset == null) {
            tagset = createLanguageTagSet(category);
            Set<String> ts = langtagSets.putIfAbsent(category, tagset);
            if (ts != null) {
                tagset = ts;
            }
        }
        return tagset;
    }

    protected Set<String> createLanguageTagSet(String category) {
        String supportedLocaleString = createSupportedLocaleString(category);
        return supportedLocaleString != null ?
            Set.of(supportedLocaleString.split("\s+")) :
            Collections.emptySet();
    }

    private static String createSupportedLocaleString(String category) {
        // Directly call Base tags, as we know it's in the base module.
        String supportedLocaleString = BaseLocaleDataMetaInfo.getSupportedLocaleString(category);

        // Use ServiceLoader to dynamically acquire installed locales' tags.
        StringBuilder tags = new StringBuilder();
        for (LocaleDataMetaInfo ldmi :
                ServiceLoader.loadInstalled(LocaleDataMetaInfo.class)) {
            if (ldmi.getType() == LocaleProviderAdapter.Type.JRE) {
                String t = ldmi.availableLanguageTags(category);
                if (t != null) {
                    if (!tags.isEmpty()) {
                        tags.append(' ');
                    }
                    tags.append(t);
                }
            }
        }
        String nonBaseTags = tags.toString();

        if (nonBaseTags != null) {
            supportedLocaleString += " " + nonBaseTags;
        }

        return supportedLocaleString;
    }

    /**
     * Lazy load available locales.
     */
    private static class AvailableJRELocales {
        private static final Locale[] localeList = createAvailableLocales();
        private AvailableJRELocales() {}
    }

    private static Locale[] createAvailableLocales() {
        /*
         * Gets the locale string list from LocaleDataMetaInfo classes and then
         * constructs the Locale array and a set of language tags based on the
         * locale string returned above.
         */
        String supportedLocaleString = createSupportedLocaleString("AvailableLocales");

        if (supportedLocaleString.isEmpty()) {
            throw new InternalError("No available locales for JRE");
        }

        return Arrays.stream(supportedLocaleString.split("\s+"))
            .map(t -> {
                return switch (t) {
                    case "ja-JP-JP" -> JRELocaleConstants.JA_JP_JP;
                    case "no-NO-NY" -> JRELocaleConstants.NO_NO_NY;
                    case "th-TH-TH" -> JRELocaleConstants.TH_TH_TH;
                    default -> Locale.forLanguageTag(t);
                };
            })
            .distinct()
            .toArray(Locale[]::new);
    }

    @Override
    public boolean isSupportedProviderLocale(Locale locale,  Set<String> langtags) {
        if (Locale.ROOT.equals(locale)) {
            return true;
        }

        locale = locale.stripExtensions();
        if (langtags.contains(locale.toLanguageTag())) {
            return true;
        }

        String oldname = locale.toString().replace('_', '-');
        return langtags.contains(oldname) ||
                   "ja-JP-JP".equals(oldname) ||
                   "th-TH-TH".equals(oldname) ||
                   "no-NO-NY".equals(oldname);
    }
}
