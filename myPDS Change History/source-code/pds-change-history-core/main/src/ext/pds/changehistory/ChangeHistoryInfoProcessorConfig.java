/*
 * Copyright 2018 PDSVision AB.
 * All rights reserved
 *
 * This software contains confidential proprietary information
 * belonging to PDSVision AB. No part of this information may be
 * used, reproduced, or stored without prior written consent
 * from PDSVision AB.
 */
package ext.pds.changehistory;

import lombok.extern.slf4j.Slf4j;
import wt.util.WTProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Martinsson
 */
@Slf4j
public class ChangeHistoryInfoProcessorConfig extends HashMap<String, String> {

    public static final String PROP_IBA_RELEASE_DATE = "ext.changeHistory.ibaReleasedDate";
    public static final String PROP_IBA_RELEASED_BY = "ext.changeHistory.ibaReleasedBy";
    public static final String PROP_IBA_CHANGE_HISTORY_INFO = "ext.changeHistory.ibaReleaseInfo";
    public static final String PROP_IBA_RELEASE_DESC = "ext.changeHistory.ibaReleaseDesc";
    public static final String PROP_IBA_REASON = "ext.changeHistory.ibaReason";
    public static final String PROP_IBA_SHORT_REASON = "ext.changeHistory.ibaShortReason";
    public static final String PROP_IBA_REASON_SOURCE = "ext.changeHistory.ibaReasonSource";
    public static final String PROP_IBA_IMPACT_TO_EXISTING_PARTS = "ext.changeHistory.ibaImpactToExistingParts";
    public static final String PROP_IBA_INFO_SERVICE = "ext.changeHistory.ibaInfoService";
    public static final String PROP_IBA_INFO_MARKETING = "ext.changeHistory.ibaInfoMarketing";

    public static final String PROP_MAX_ENTRIES = "ext.changeHistory.max";
    public static final String PROP_DOCTYPE = "ext.changeHistory.docType";
    public static final String PROP_DATE_FORMAT = "ext.changeHistory.dateFormat";

    public static final String PROP_FILTER_ON_CATEGORY = "ext.changeHistory.filterCategory";
    public static final String PROP_RESOLVED_CN_STATE = "ext.changeHistory.resolvedCNState";

    public static final String PROP_GET_REASON_FROM_CHANGE = "ext.changeHistory.getReasonFromChange";

    private static final Map<String, String> tags = new HashMap<>();

    static {
        tags.put(PROP_IBA_CHANGE_HISTORY_INFO, "changeHistory");
        tags.put(PROP_IBA_RELEASE_DATE, "releaseDate");
        tags.put(PROP_IBA_RELEASED_BY, "releasedBy");
        tags.put(PROP_IBA_REASON, "reason");
        tags.put(PROP_IBA_REASON_SOURCE, "reasonSource");
        tags.put(PROP_IBA_SHORT_REASON, "shortReason");
        tags.put(PROP_IBA_INFO_SERVICE, "infoService");
        tags.put(PROP_IBA_INFO_MARKETING, "infoMarketing");
        tags.put(PROP_IBA_IMPACT_TO_EXISTING_PARTS, "impactToExistingParts");
        tags.put(PROP_FILTER_ON_CATEGORY, "false");
        tags.put(PROP_RESOLVED_CN_STATE, "RESOLVED");
        tags.put(PROP_IBA_RELEASE_DESC, "releaseDesc");
        tags.put(PROP_GET_REASON_FROM_CHANGE, "true");
    }

    public static ChangeHistoryInfoProcessorConfig newServerSideConfig() {

        ChangeHistoryInfoProcessorConfig config = new ChangeHistoryInfoProcessorConfig();
        WTProperties props;

        try {
            props = WTProperties.getLocalProperties();

            set(PROP_DATE_FORMAT, "yyyy-MM-dd", config, props);
            set(PROP_MAX_ENTRIES, "8", config, props);
            set(PROP_DOCTYPE, "CADDRAWING", config, props);
            set(PROP_IBA_CHANGE_HISTORY_INFO, "releaseInfo", config, props);
            set(PROP_IBA_RELEASE_DATE, "PDS_CHANGE_HISTORY_RELEASE_DATE", config, props);
            set(PROP_IBA_RELEASED_BY, "PDS_CHANGE_HISTORY_RELEASED_BY", config, props);
            set(PROP_IBA_REASON, "PDS_CHANGE_HISTORY_REASON", config, props);
            set(PROP_IBA_REASON_SOURCE, "PDS_CHANGE_HISTORY_REASON_SOURCE", config, props);
            set(PROP_IBA_SHORT_REASON, "PDS_CHANGE_HISTORY_SHORT_REASON", config, props);
            set(PROP_IBA_INFO_SERVICE, "PDS_CHANGE_HISTORY_INFO_SERVICE", config, props);
            set(PROP_IBA_INFO_MARKETING, "PDS_CHANGE_HISTORY_INFO_MARKETING", config, props);
            set(PROP_IBA_IMPACT_TO_EXISTING_PARTS, "PDS_CHANGE_HISTORY_IMPACT_TO_EXISTING_PARTS", config, props);
            set(PROP_FILTER_ON_CATEGORY, "false", config, props);
            set(PROP_RESOLVED_CN_STATE, "RESOLVED", config, props);
            set(PROP_IBA_RELEASE_DESC, "releaseDesc", config, props);
            set(PROP_GET_REASON_FROM_CHANGE, "true", config, props);

            return config;
        } catch (IOException ex) {
            throw new IllegalStateException("Was unable to read wt.properties", ex);
        }

    }


    /**
     * Returns the tag associated with the given property key. If no tag is defined,
     * the key itself is returned.
     * <p>
     * This method is used in the Velocity template file ext/pds/changehistory/report/pn-report.xsl
     * to resolve configuration property keys into readable tag names.
     *
     * @param key the property key to look up
     * @return the corresponding tag name, or the key itself if no mapping exists
     */
    public String getTag(String key) {
        if (log.isTraceEnabled()) {
            log.trace("getTag(key={})", key);
            log.trace("key: {}", key);
        }
        String tag = tags.getOrDefault(key, key);
        log.trace("getTag#return: {}", tag);
        return tag;
    }

    public String getAttr(String key) {
        return containsKey(key) ? super.get(key) : key;
    }

    private static void set(String key, String defaultValue, ChangeHistoryInfoProcessorConfig config, WTProperties props) {
        config.put(key, props.getProperty(key, defaultValue));
    }

}
