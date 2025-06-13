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
import wt.series.MultilevelSeries;
import wt.series.SeriesException;
import wt.vc.Mastered;

import java.util.Comparator;

/**
 * @author AKullenberg
 */


public class RevisionComparator implements Comparator<String> {

    private static final Logger logger = LoggerFactory.getLogger(RevisionComparator.class);
    private final String series;

    public RevisionComparator(Mastered m) {
        series = m.getSeries();
    }

    @Override
    public int compare(String o1, String o2) {
        try {
            MultilevelSeries rev1 = MultilevelSeries.newMultilevelSeries(series, o1);
            MultilevelSeries rev2 = MultilevelSeries.newMultilevelSeries(series, o2);
            logger.trace("Revisions to compare {} to {}", o1, o2);

            if (rev1.equals(rev2)) {
                logger.trace("Revisions are equal");
                return 0;
            }

            if (rev1.greaterThan(rev2)) {
                logger.trace("Revision {} greater than {}", o1, o2);
                return 1;
            }

            logger.trace("Revision {} less than {}", o1, o2);
            return -1;

        } catch (SeriesException ex) {
            logger.error("Could not get Revision label, values added on top of each other instead of sorted", ex);
            return 1;
        }
    }
}
