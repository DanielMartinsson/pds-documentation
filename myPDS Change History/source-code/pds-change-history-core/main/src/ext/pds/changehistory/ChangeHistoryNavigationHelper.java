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
import wt.change2.ChangeHelper2;
import wt.change2.WTChangeOrder2;
import wt.enterprise.RevisionControlled;
import wt.epm.EPMDocument;
import wt.fc.QueryResult;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTKeyedHashMap;
import wt.lifecycle.State;
import wt.lifecycle._State;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.util.WTException;

import java.util.Iterator;

/**
 * @author Kullenberg
 */

public class ChangeHistoryNavigationHelper {

    private static final Logger logger = LoggerFactory.getLogger(ChangeHistoryNavigationHelper.class);
    private static final State APPROVED = _State.toState("APPROVED");

    private ChangeHistoryNavigationHelper() {
        // Hides implicit no-args constructor
    }

    public static WTChangeOrder2 getLatestCN(EPMDocument epm, State state, boolean matchState) throws WTException {
        QueryResult changeOrders;
        WTChangeOrder2 cn = null;
        changeOrders = ChangeHelper2.service.getLatestUniqueImplementedChangeOrders(epm);

        if (changeOrders != null) {
            // How to filter multiple CNs with same state?
            while (changeOrders.hasMoreElements()) {
                WTChangeOrder2 candidate = (WTChangeOrder2) changeOrders.nextElement();
                if (!matchState || state.equals(candidate.getLifeCycleState())) {
                    cn = candidate;
                    break;
                }
            }
        }
        return cn;
    }

    public static PromotionNotice getApprovedPN(Object promotable, State state) throws WTException {
        if (logger.isTraceEnabled()) {
            logger.trace("getApprovedPN(promotable={}, state={})", toString(promotable), state);
        }

        if (promotable == null) {
            return null;
        }

        PromotionNotice pn = null;

        RevisionControlled rc = (RevisionControlled) promotable;
        State targetState = rc.getLifeCycleState();
        // Returns a map of size one where the promotable is the key and the value is a set of related PNs
        WTKeyedHashMap pns = findPromotionNotices(promotable, state);
        WTHashSet candidates = (WTHashSet) pns.get(promotable);
        Iterator itr = candidates.persistableIterator();

        while (pn == null && itr.hasNext()) {
            PromotionNotice candidate = (PromotionNotice) itr.next();

            if (!APPROVED.equals(candidate.getLifeCycleState())) {
                continue;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Found approved candidate PN {} for {}", candidate.getNumber(), toString(rc));
            }

            if (targetState.equals(candidate.getMaturityState())) {
                logger.debug("Candidate PN {} has valid maturity state {}", candidate.getNumber(), targetState.getDisplay());
                pn = candidate;
            }

        }

        return pn;
    }

    public static WTKeyedHashMap findPromotionNotices(Object obj, State state) throws WTException {
        if (logger.isTraceEnabled()) {
            logger.trace("findPromotionNotices(Object obj, State state)");
            logger.trace("obj: {}", toString(obj));
            logger.trace("state: {}", state);
        }
        WTHashSet wthashset = new WTHashSet();
        wthashset.addElement(obj);
        return MaturityHelper.service.getPromotionNotices(wthashset, state);
    }

    private static String toString(Object obj) {
        String str = null;
        if (obj != null) {
            str = obj.toString();
            if (obj instanceof PromotionNotice) {
                PromotionNotice pn = (PromotionNotice) obj;
                str = pn.getName() + " " + pn.getState();
            } else if (obj instanceof EPMDocument) {
                EPMDocument doc = (EPMDocument) obj;
                str = doc.getCADName() + " " + doc.getIterationDisplayIdentifier() + " " + doc.getState();
            }
        }
        return str;
    }
}
