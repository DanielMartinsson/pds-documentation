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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wt.epm.EPMDocument;
import wt.util.WTProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author AKullenberg
 */


public class ChangeHistoryAttributeHelper {

    private static final Logger logger = LoggerFactory.getLogger(ChangeHistoryAttributeHelper.class);
    private final ChangeHistoryProcessor changeHistoryProcessor;

    private String chIndex = "CH_INDEX_";
    private String chDesc = "CH_DESC_";
    private String chDate = "CH_DATE_";
    private String chUser = "CH_USER_";

    public ChangeHistoryAttributeHelper() {
        changeHistoryProcessor = ChangeHistoryProcessor.newProcessor();

        try {
            WTProperties props = WTProperties.getLocalProperties();
            chIndex = props.getProperty("ext.changeHistory.ch_index.parameter", "CH_INDEX_");
            chDesc = props.getProperty("ext.changeHistory.ch_desc.parameter", "CH_DESC_");
            chDate = props.getProperty("ext.changeHistory.ch_date.parameter", "CH_DATE_");
            chUser = props.getProperty("ext.changeHistory.ch_user.parameter", "CH_USER_");
        } catch (IOException ex) {
            logger.warn("Not able to read wt.properties, using default Parameter/Property names", ex);
        }

    }

    public static ChangeHistoryAttributeHelper newHelper() {
        return new ChangeHistoryAttributeHelper();
    }

    public Map<String, String> getChangeHistoryInfo(EPMDocument epm) {
        logger.trace("getChangeHistoryInfo(EPMDocument epm={})", epm.getNumber());

        HashMap<String, String> map = new HashMap<>();
        ArrayList<ChangeHistoryInfo> infoList = new ArrayList<>();
        infoList.addAll(changeHistoryProcessor.getStoredChangeHistory(epm));
        infoList.addAll(changeHistoryProcessor.getOngoingReleaseInfo(epm));

        for (int i = 0; i < infoList.size(); i++) {
            add(i, infoList.get(i), map);
        }

        return map;
    }

    private void add(int index, ChangeHistoryInfo entry, HashMap<String, String> map) {
        add(chIndex + index, entry.getRevision().toLowerCase(), map);
        add(chDesc + index, entry.getReason(), map);
        add(chDate + index, entry.getDate(), map);
        add(chUser + index, entry.getReleaseBy(), map);
    }

    private void add(String key, String value, HashMap<String, String> map) {
        map.put(key, value);
    }
}
