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

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author Daniel Södling
 */
@Setter
@Getter
public class ChangeHistoryInfo {

    private static final Logger logger = LoggerFactory.getLogger(ChangeHistoryInfo.class);

    public static final String DELIM = "|";

    private String revision;
    private String reason;
    private String date;
    private String releaseBy;

    public static ChangeHistoryInfo newChangeHistoryInfo(String revision, String reason, String date, String releaseBy) {
        ChangeHistoryInfo info = new ChangeHistoryInfo();
        info.revision = revision;
        info.reason = reason;
        info.date = date;
        info.releaseBy = releaseBy;
        return info;
    }

    public static ChangeHistoryInfo parse(String str) {
        logger.trace("parse(str={})", str);

        ChangeHistoryInfo info = new ChangeHistoryInfo();
        String[] values = str.split("\\|");

        if (logger.isDebugEnabled()) {
            logger.debug("tokens: {}", Arrays.toString(values));
        }

        try {
            info.revision = values[0];
            info.reason = values[1];
            info.date = values[2];
            info.releaseBy = values[3];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Invalid entry format: " + str);
        }

        return info;
    }

    public String format() {
        StringBuilder str = new StringBuilder();
        str.append(revision).append(DELIM);
        str.append(reason).append(DELIM);
        str.append(date).append(DELIM);
        str.append(releaseBy);
        return str.toString();
    }

    @Override
    public String toString() {
        return format();
    }

}
