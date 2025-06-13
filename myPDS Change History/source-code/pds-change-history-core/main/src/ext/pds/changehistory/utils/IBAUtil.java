/*
 * Copyright 2025 PDSVISION.
 * All rights reserved
 *
 * This software contains confidential proprietary information
 * belonging to PDSVISION. No part of this information may be
 * used, reproduced, or stored without prior written consent
 * from PDSVISION.
 */
package ext.pds.changehistory.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wt.epm.EPMObject;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceManager;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.iba.definition.AbstractAttributeDefinition;
import wt.iba.definition.DefinitionLoader;
import wt.iba.definition.FloatDefinition;
import wt.iba.definition.litedefinition.AbstractAttributeDefinizerView;
import wt.iba.definition.litedefinition.AttributeDefDefaultView;
import wt.iba.definition.litedefinition.AttributeDefNodeView;
import wt.iba.definition.litedefinition.BooleanDefView;
import wt.iba.definition.litedefinition.FloatDefView;
import wt.iba.definition.litedefinition.IntegerDefView;
import wt.iba.definition.litedefinition.RatioDefView;
import wt.iba.definition.litedefinition.ReferenceDefView;
import wt.iba.definition.litedefinition.StringDefView;
import wt.iba.definition.litedefinition.TimestampDefView;
import wt.iba.definition.litedefinition.URLDefView;
import wt.iba.definition.litedefinition.UnitDefView;
import wt.iba.definition.service.IBADefinitionHelper;
import wt.iba.value.AbstractContextualValue;
import wt.iba.value.DefaultAttributeContainer;
import wt.iba.value.FloatValue;
import wt.iba.value.IBAHolder;
import wt.iba.value.IBAValueUtility;
import wt.iba.value.StringValue;
import wt.iba.value.litevalue.AbstractValueView;
import wt.iba.value.litevalue.BooleanValueDefaultView;
import wt.iba.value.litevalue.TimestampValueDefaultView;
import wt.iba.value.service.IBAValueDBService;
import wt.iba.value.service.IBAValueHelper;
import wt.iba.value.service.LoadValue;
import wt.pds.StatementSpec;
import wt.query.QueryException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.session.SessionManager;
import wt.units.service.QuantityOfMeasureDefaultView;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;

/**
 * @author Daniel Martinsson
 */

public class IBAUtil {

    private final PersistenceManager persistenceManager;
    private final SessionManager sessionManager;
    private Hashtable ibaContainer;
    private Hashtable ibaStringContainer;
    private Vector epmConstraintParam;
    private IBAHolder ibaholder;
    private boolean committed = false;

    private static final Logger logger = LoggerFactory.getLogger(IBAUtil.class.getName());

    /**
     * Convenience method for getting a IBA value by only providing OBID and
     * IBA name
     *
     * @param obid
     * @param ibaName
     * @return
     */
    public static String getIBAValue(String obid, String ibaName) {
        ReferenceFactory rf = new ReferenceFactory();

        IBAHolder holder;
        try {
            holder = (IBAHolder) rf.getReference(obid).getObject();
        } catch (WTException ex) {
            throw new RuntimeException("Unable to create IBAUtil", ex);
        }
        return newIBAUtil(holder, true).getIBAValue(ibaName);
    }


    /**
     * Creates a new instance of the <code>IBAUtil</code> class using the
     * default (PDMLink) instances for the <code>PersistenceManager</code> and
     * <code>SessionManager</code> dependencies.
     *
     * @param holder
     * @param overrideImmutableConstraint
     * @return
     */
    public static IBAUtil newIBAUtil(IBAHolder holder, boolean overrideImmutableConstraint) {
        try {
            return new IBAUtil(PersistenceHelper.manager, SessionHelper.manager, holder, overrideImmutableConstraint);
        } catch (WTException ex) {
            throw new RuntimeException("Unable to create IBAUtil", ex);
        } catch (RemoteException ex) {
            throw new RuntimeException("Unable to create IBAUtil", ex);
        }
    }

    /**
     * Creates an IBAUtil instance linked to an IBAHolder with an option to
     * override the immutable constraint set automatically on family table
     * members.
     *
     * @param persistenceManager
     * @param sessionManager
     * @param ibaholder                   The IBAHolder to use for setting IBA values
     * @param overrideImmutableConstraint true to override the immutable
     *                                    constraint, false to act with default behavior
     * @throws WTException
     * @throws RemoteException
     */
    public IBAUtil(PersistenceManager persistenceManager, SessionManager sessionManager,
                   IBAHolder ibaholder, boolean overrideImmutableConstraint) throws WTException, RemoteException {

        if (logger.isTraceEnabled()) {
            logger.trace("IBAUtil(PersistenceManager persistenceManager, SessionManager sessionManager, IBAHolder ibaholder, boolean overrideImmutableConstraint)");
            logger.trace("persistenceManager: " + persistenceManager);
            logger.trace("sessionManager: " + sessionManager);
            logger.trace("ibaholder: " + LogHelper.toString(ibaholder));
            logger.trace("overrideImmutableConstraint: " + overrideImmutableConstraint);
        }

        this.persistenceManager = persistenceManager;
        this.sessionManager = sessionManager;
        this.ibaholder = ibaholder;
        this.ibaContainer = new Hashtable();
        initializeIBAPart(ibaholder, overrideImmutableConstraint);
        initializeIBAPartStringValue(ibaholder);

    }

    /**
     * Creates and returns a string showing all IBA values for the current
     * holder.
     *
     * @return a string
     */
    public String printIBAValues() {

        if (logger.isTraceEnabled()) {
            logger.trace("printIBAValues()");
        }

        StringBuilder sb = new StringBuilder();
        Enumeration enumeration = ibaContainer.keys();

        try {

            while (enumeration.hasMoreElements()) {

                String s = (String) enumeration.nextElement();
                AbstractValueView abstractvalueview = (AbstractValueView) ((Object[]) ibaContainer.get(s))[1];
                sb.append(s).append(" - ");
                sb.append(IBAValueUtility.getLocalizedIBAValueDisplayString(abstractvalueview, sessionManager.getLocale()));
                sb.append('\n');

            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }

        return sb.toString();
    }

    public String getIBAValue(String s) {

        if (logger.isTraceEnabled()) {
            logger.trace("getIBAValue(String s)");
            logger.trace("s: " + s);
        }

        try {
            return getIBAValue(s, sessionManager.getLocale());
        } catch (Exception exception) {
            exception.printStackTrace(System.out);
        }
        return null;
    }

    public String getIBAValue(String s, Locale locale) {

        if (logger.isTraceEnabled()) {
            logger.trace("getIBAValue(String s, Locale locale)");
            logger.trace("s: " + s);
            logger.trace("locale: " + locale);
        }

        try {
            if (ibaContainer.get(s) == null) {
                return null;
            } else {
                AbstractValueView abstractvalueview = (AbstractValueView) ((Object[]) ibaContainer.get(s))[1];
                return IBAValueUtility.getLocalizedIBAValueDisplayString(abstractvalueview, locale);
            }
        } catch (WTException wtexception) {
            wtexception.printStackTrace(System.out);
        }
        return null;
    }

    public List<String> getIBAValues(String s) {

        if (logger.isTraceEnabled()) {
            logger.trace("getIBAValues(String s)");
            logger.trace("s: " + s);
        }

        List<String> result = new ArrayList();
        Locale locale;
        try {
            locale = sessionManager.getLocale();
            if (ibaContainer.get(s) == null) {
                return null;
            } else {
                Object[] objects = (Object[]) ibaContainer.get(s);
                for (Object obj : Arrays.asList(objects).subList(1, objects.length)) {
                    AbstractValueView abstractvalueview = (AbstractValueView) obj;
                    result.add(IBAValueUtility.getLocalizedIBAValueDisplayString(abstractvalueview, locale));
                }
            }
        } catch (WTException ex) {
            java.util.logging.Logger.getLogger(IBAUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public String getIBAStringValue(String s) {

        if (logger.isTraceEnabled()) {
            logger.trace("getIBAStringValue(String s)");
            logger.trace("s: " + s);
        }

        return (String) ibaStringContainer.get(s);

    }

    private void initializeIBAPart(IBAHolder ibaholder, boolean overrideImmutableConstraint)
            throws WTException, RemoteException {

        if (logger.isTraceEnabled()) {
            logger.trace("initializeIBAPart(IBAHolder ibaholder, boolean overrideImmutableConstraint)");
            logger.trace("ibaholder: " + LogHelper.toString(ibaholder));
            logger.trace("overrideImmutableConstraint: " + overrideImmutableConstraint);
        }

        ibaContainer = new Hashtable();
        ibaholder = IBAValueHelper.service.refreshAttributeContainer(ibaholder, null, sessionManager.getLocale(), null);
        DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) ibaholder.getAttributeContainer();

        if (defaultattributecontainer != null) {
            if (ibaholder instanceof EPMObject && overrideImmutableConstraint) {
                IBAValueDBService ibavaluedbservice = new IBAValueDBService();
                Object consParam = defaultattributecontainer.getConstraintParameter();
                Object tempParam = new wt.epm.attributes.EPMIBAConstraintFactory.EditFileBasedAttributes();
                if (consParam instanceof Vector) {
                    epmConstraintParam = (Vector) consParam;
                } else {
                    epmConstraintParam = new Vector();
                    epmConstraintParam.add(consParam);
                }
                if (!epmConstraintParam.contains(tempParam)) {
                    epmConstraintParam.add(tempParam);
                }
                defaultattributecontainer = (DefaultAttributeContainer) ibavaluedbservice.refreshAttributeConstraint(ibaholder, epmConstraintParam, null);

            } else {
                //System.out.println("Oops... "+ibaholder.getClass().getName());
            }
            AttributeDefDefaultView aattributedefdefaultview[] = defaultattributecontainer.getAttributeDefinitions();
            for (int i = 0; i < aattributedefdefaultview.length; i++) {
                AbstractValueView aabstractvalueview[] = defaultattributecontainer.getAttributeValues(aattributedefdefaultview[i]);
                if (aabstractvalueview != null) {
                    Object aobj[] = new Object[aabstractvalueview.length + 1];
                    aobj[0] = aattributedefdefaultview[i];
                    for (int j = 1; j <= aabstractvalueview.length; j++) {
                        aobj[j] = aabstractvalueview[j - 1];
                    }

                    ibaContainer.put(aattributedefdefaultview[i].getName(), ((Object) (aobj)));
                }
            }

        }
    }

    public Enumeration getAttributeDefinitions() {

        if (logger.isTraceEnabled()) {
            logger.trace("getAttributeDefinitions()");
        }

        return ibaContainer.keys();
    }

    public String getIBAValue(IBAHolder ibaholder, String s) {

        if (logger.isTraceEnabled()) {
            logger.trace("getIBAValue(IBAHolder ibaholder, String s)");
            logger.trace("ibaholder: " + ibaholder);
            logger.trace("s: " + s);
        }

        String s1 = null;
        try {
            ibaholder = IBAValueHelper.service.refreshAttributeContainer(ibaholder, null, sessionManager.getLocale(), null);
            DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) ibaholder.getAttributeContainer();
            if (defaultattributecontainer != null) {
                AttributeDefDefaultView aattributedefdefaultview[] = defaultattributecontainer.getAttributeDefinitions();
                for (int i = 0; i < aattributedefdefaultview.length; i++) {
                    if (aattributedefdefaultview[i].getName().equals(s)) {
                        AbstractValueView aabstractvalueview[] = defaultattributecontainer.getAttributeValues(aattributedefdefaultview[i]);
                        if (aabstractvalueview != null) {
                            for (int j = 0; j < aabstractvalueview.length; j++) {
                                if (s1 == null) {
                                    s1 = IBAValueUtility.getLocalizedIBAValueDisplayString(aabstractvalueview[j], sessionManager.getLocale());
                                } else {
                                    s1 = s1 + "," + IBAValueUtility.getLocalizedIBAValueDisplayString(aabstractvalueview[j], sessionManager.getLocale());
                                }
                            }

                        }
                    }
                }

            }
        } catch (Exception exception) {
            exception.printStackTrace(System.out);
        }
        return s1;
    }

    private void initializeIBAPartStringValue(IBAHolder ibaholder)
            throws WTException, RemoteException {

        if (logger.isTraceEnabled()) {
            logger.trace("initializeIBAPartStringValue(IBAHolder ibaholder)");
            logger.trace("ibaHolder: " + LogHelper.toString(ibaholder));
        }

        ibaStringContainer = new Hashtable();
        Object aobj[] = new Object[2];
        ibaholder = IBAValueHelper.service.refreshAttributeContainer(ibaholder, null, sessionManager.getLocale(), null);
        DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) ibaholder.getAttributeContainer();
        if (defaultattributecontainer != null) {
            AttributeDefDefaultView aattributedefdefaultview[] = defaultattributecontainer.getAttributeDefinitions();
            for (int i = 0; i < aattributedefdefaultview.length; i++) {
                AbstractValueView aabstractvalueview[] = defaultattributecontainer.getAttributeValues(aattributedefdefaultview[i]);
                if (aabstractvalueview != null) {
                    aobj[0] = aattributedefdefaultview[i];
                    aobj[1] = IBAValueUtility.getLocalizedIBAValueDisplayString(aabstractvalueview[0], sessionManager.getLocale());
                    for (int j = 1; j < aabstractvalueview.length; j++) {
                        aobj[1] = aobj[1] + "," + IBAValueUtility.getLocalizedIBAValueDisplayString(aabstractvalueview[j], sessionManager.getLocale());
                    }

                }
                ibaStringContainer.put(aattributedefdefaultview[i].getName(), aobj[1]);
            }

        }
    }

    public Hashtable<String, String> getAllIBAs() {
        return ibaStringContainer;
    }

    public DefaultAttributeContainer removeCSMConstraint(DefaultAttributeContainer defaultattributecontainer) {

        if (logger.isTraceEnabled()) {
            logger.trace("removeCSMConstraint(DefaultAttributeContainer defaultattributecontainer)");
            logger.trace("defaultattributecontainer");
        }

        String CSM = "CSM";
        Object obj = defaultattributecontainer.getConstraintParameter();

        if (obj == null) {
            obj = CSM;
        } else if (obj instanceof Vector) {
            ((Vector) obj).addElement(CSM);
        } else {
            Vector vector = new Vector();
            vector.addElement(obj);
            obj = vector;
            ((Vector) obj).addElement(CSM);
        }
        try {
            defaultattributecontainer.setConstraintParameter(obj);
        } catch (WTPropertyVetoException wtpropertyvetoexception) {
            wtpropertyvetoexception.printStackTrace(System.out);
        }
        return defaultattributecontainer;
    }


    public IBAHolder updatePartAttributeContainer(IBAHolder ibaholder)
            throws WTException, WTPropertyVetoException, RemoteException {
        ibaholder = IBAValueHelper.service.refreshAttributeContainer(ibaholder, null, SessionHelper.manager.getLocale(), null);
        DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) ibaholder.getAttributeContainer();
        defaultattributecontainer = (DefaultAttributeContainer) (new IBAValueDBService()).refreshAttributeConstraint(ibaholder, epmConstraintParam, null);

        for (Enumeration enumeration = ibaContainer.elements(); enumeration.hasMoreElements(); ) {
            Object aobj[] = (Object[]) enumeration.nextElement();
            AttributeDefDefaultView attributedefdefaultview = (AttributeDefDefaultView) aobj[0];
            for (int i = 1; i < aobj.length; i++) {
                AbstractValueView abstractvalueview = (AbstractValueView) aobj[i];
                if (abstractvalueview.getState() == 1) {
                    defaultattributecontainer.deleteAttributeValues(attributedefdefaultview);
                    abstractvalueview.setState(3);
                    defaultattributecontainer.addAttributeValue(abstractvalueview);
                } else if (abstractvalueview.getState() == 3) {
                    defaultattributecontainer.addAttributeValue(abstractvalueview);
                } else if (abstractvalueview.getState() == 2) {
                    defaultattributecontainer.deleteAttributeValue(abstractvalueview);
                }
            }

        }

        ibaholder.setAttributeContainer(defaultattributecontainer);
        return ibaholder;
    }

    public IBAHolder updateIBAPart(IBAHolder ibaholder)
            throws WTException, WTPropertyVetoException, RemoteException {

        if (logger.isTraceEnabled()) {
            logger.trace("updateIBAPart(IBAHolder ibaholder)");
            logger.trace("ibaHolder: " + LogHelper.toString(ibaholder));
        }

        return updatePartAttributeContainer(ibaholder);
    }

    public boolean updateIBAHolder(IBAHolder ibaholder) {

        if (logger.isTraceEnabled()) {
            logger.trace("updateIBAHolder(IBAHolder ibaholder)");
            logger.trace("ibaHolder: " + LogHelper.toString(ibaholder));
        }

        IBAValueDBService ibavaluedbservice = new IBAValueDBService();
        boolean flag = true;
        try {
            // This causes the timestamp to be updated. The actual updating of attributes seems to work just fine without it.
            //PersistenceServerHelper.manager.update((Persistable) ibaholder);
            wt.iba.value.AttributeContainer attributecontainer = ibaholder.getAttributeContainer();
            Object obj = this.epmConstraintParam == null ? ((DefaultAttributeContainer) attributecontainer).getConstraintParameter() : this.epmConstraintParam;
            wt.iba.value.AttributeContainer attributecontainer1 = ibavaluedbservice.updateAttributeContainer(ibaholder, obj, null, null);
            ibaholder.setAttributeContainer(attributecontainer1);
        } catch (WTException wtexception) {
            logger.error("updateIBAHolder: Couldn't update. ", wtexception);
            flag = false;
        }
        return flag;
    }

    /**
     * Sets the value of the given iba.
     *
     * @param s  The name of the iba.
     * @param s1 The value to set the iba to.
     * @throws WTPropertyVetoException
     */
    public void setIBAValue(String s, String s1) throws WTPropertyVetoException {

        if (logger.isTraceEnabled()) {
            logger.trace("setIBAValue(String s, String s1)");
            logger.trace("s: " + s);
            logger.trace("s1: " + s1);
        }

        AbstractValueView abstractvalueview = null;
        AttributeDefDefaultView attributedefdefaultview = null;
        Object aobj[] = (Object[]) ibaContainer.get(s);
        if (aobj != null) {
            abstractvalueview = (AbstractValueView) aobj[1];
            attributedefdefaultview = (AttributeDefDefaultView) aobj[0];
        }
        if (abstractvalueview == null) {
            attributedefdefaultview = getAttributeDefinition(s);
        }
        if (attributedefdefaultview == null) {
            return;
        }
        if (attributedefdefaultview instanceof UnitDefView) {
            s1 = s1 + " " + getDisplayUnits((UnitDefView) attributedefdefaultview, "SI");
        }
        abstractvalueview = internalCreateValue(attributedefdefaultview, s1);
        if (abstractvalueview == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("The abstractvalueview is null, returning.");
            }
            return;
        } else {

            abstractvalueview.setState(1);
            Object aobj1[] = new Object[2];
            aobj1[0] = attributedefdefaultview;
            aobj1[1] = abstractvalueview;
            ibaContainer.put(attributedefdefaultview.getName(), ((Object) (aobj1)));
            return;
        }
    }

    /**
     * Update the value of a Timestamp IBA
     *
     * @param ibaName  The internal name of the IBA
     * @param ibaValue The Timestamp value
     * @throws WTException
     * @throws java.rmi.RemoteException
     * @throws wt.util.WTPropertyVetoException
     */
    public void setIBATimestampValue(String ibaName, Timestamp ibaValue) throws WTException, RemoteException, WTPropertyVetoException {

        if (logger.isTraceEnabled()) {
            logger.trace("setIBATimestampValue(String ibaName, Timestamp ibaValue)");
            logger.trace("ibaName: " + ibaName);
            logger.trace("ibaValue: " + ibaValue);
        }
        if (ibaValue == null) {
            return;
        }
        AbstractValueView abstractvalueview = null;
        AttributeDefDefaultView attributedefdefaultview = null;
        Object aobj[] = (Object[]) ibaContainer.get(ibaName);
        if (aobj != null) {
            abstractvalueview = (AbstractValueView) aobj[1];
            attributedefdefaultview = (AttributeDefDefaultView) aobj[0];
        }
        if (abstractvalueview == null) {
            attributedefdefaultview = getAttributeDefinition(ibaName);
        }
        if (attributedefdefaultview == null) {
            return;
        }
        DefaultAttributeContainer attribContainer = (DefaultAttributeContainer) ibaholder.getAttributeContainer(); // current IBAs of the IBAHolder
        if (attribContainer != null) {
            AbstractAttributeDefinizerView timestampDefinizer = null;
            timestampDefinizer = IBADefinitionHelper.service.getAttributeDefDefaultViewByPath(ibaName);
            abstractvalueview = new TimestampValueDefaultView((TimestampDefView) timestampDefinizer, ibaValue);
        }

        if (abstractvalueview != null) {
            abstractvalueview.setState(1);
            Object aobj1[] = new Object[2];
            aobj1[0] = attributedefdefaultview;
            aobj1[1] = abstractvalueview;
            ibaContainer.put(attributedefdefaultview.getName(), ((Object) (aobj1)));
        } else if (logger.isTraceEnabled()) {
            logger.trace("setIBATimestampValue() The abstractvalueview is null, returning.");
        }
    }

    /**
     * Update the value of a Boolean IBA
     *
     * @param ibaName  The internal name of the IBA
     * @param ibaValue The Boolean value
     * @throws WTException
     * @throws java.rmi.RemoteException
     * @throws wt.util.WTPropertyVetoException
     */
    public void setIBABooleanValue(String ibaName, Boolean ibaValue) throws WTException, RemoteException, WTPropertyVetoException {

        if (logger.isTraceEnabled()) {
            logger.trace("setIBABooleanValue(String ibaName, Boolean ibaValue)");
            logger.trace("ibaName: " + ibaName);
            logger.trace("ibaValue: " + ibaValue);
        }
        if (ibaValue == null) {
            return;
        }
        AbstractValueView abstractvalueview = null;
        AttributeDefDefaultView attributedefdefaultview = null;
        Object aobj[] = (Object[]) ibaContainer.get(ibaName);
        if (aobj != null) {
            abstractvalueview = (AbstractValueView) aobj[1];
            attributedefdefaultview = (AttributeDefDefaultView) aobj[0];
        }
        if (abstractvalueview == null) {
            attributedefdefaultview = getAttributeDefinition(ibaName);
        }
        if (attributedefdefaultview == null) {
            logger.trace("setIBABooleanValue() The attributedefdefaultview is null, returning.");
            return;
        }
        DefaultAttributeContainer attribContainer = (DefaultAttributeContainer) ibaholder.getAttributeContainer(); // current IBAs of the IBAHolder
        if (attribContainer != null) {
            AbstractAttributeDefinizerView booleanDefinizer = null;
            booleanDefinizer = IBADefinitionHelper.service.getAttributeDefDefaultViewByPath(ibaName);
            abstractvalueview = new BooleanValueDefaultView((BooleanDefView) booleanDefinizer, ibaValue);
        }

        if (abstractvalueview != null) {
            abstractvalueview.setState(1);
            Object aobj1[] = new Object[2];
            aobj1[0] = attributedefdefaultview;
            aobj1[1] = abstractvalueview;
            ibaContainer.put(attributedefdefaultview.getName(), ((Object) (aobj1)));
        } else if (logger.isTraceEnabled()) {
            logger.trace("setIBABooleanValue() The abstractvalueview is null, returning.");
        }
    }

    /**
     * Sets the provided values on the iba with the provided name. This method
     * should not be used, use the method: setIBAValueSet(String ibaName, List valueList) instead.
     *
     * @param s
     * @param vector
     * @throws WTPropertyVetoException
     * @throws Exception
     */
    public void setIBAValueSet(String s, Vector vector) throws WTPropertyVetoException, Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("setIBAValueSet(String s, Vector vector)");
            logger.trace("s: " + s);
            logger.trace("vector: " + vector);
        }

        AbstractValueView abstractvalueview = null;
        AttributeDefDefaultView attributedefdefaultview = null;
        Object aobj[] = (Object[]) ibaContainer.get(s);
        if (aobj != null) {
            abstractvalueview = (AbstractValueView) aobj[1];
            attributedefdefaultview = (AttributeDefDefaultView) aobj[0];
        }
        if (abstractvalueview == null) {
            attributedefdefaultview = getAttributeDefinition(s);
        }
        if (attributedefdefaultview == null) {
            return;
        }
        Object aobj1[] = (Object[]) ibaContainer.get(attributedefdefaultview.getName());
        Object aobj2[] = new Object[vector.size() + 1];
        aobj2[0] = attributedefdefaultview;
        for (int i = 1; i <= vector.size(); i++) {
            Object aobj3[] = (Object[]) vector.elementAt(i - 1);
            if (attributedefdefaultview instanceof UnitDefView) {
                aobj3[0] = aobj3[0] + " " + getDisplayUnits((UnitDefView) attributedefdefaultview, "SI");
            }
            AbstractValueView abstractvalueview1 = internalCreateValue(attributedefdefaultview, (String) aobj3[0]);
            for (int j = 1; j < aobj1.length; j++) {
                if (((AbstractValueView) aobj1[j]).compareTo(abstractvalueview1) == 0) {
                    abstractvalueview1 = (AbstractValueView) aobj1[j];
                }
            }

            if (abstractvalueview1 == null) {
                return;
            }
            abstractvalueview1.setState(((Integer) aobj3[1]).intValue());
            aobj2[i] = abstractvalueview1;
        }

        ibaContainer.put(attributedefdefaultview.getName(), ((Object) (aobj2)));
    }

    /**
     * Sets the provided values on the multi valued IBA with the provided name.
     *
     * @param ibaName   The name of the multivalued IBA to populate.
     * @param valueList The list of values that will be added to the IBA with
     *                  the provided name.
     * @throws WTPropertyVetoException
     * @throws Exception
     */
    public void setIBAValueSet(String ibaName, List<String> valueList) throws WTPropertyVetoException, Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("setIBAValueSet(String s, List<String> valueList) throws WTPropertyVetoException, Exception");
            logger.trace("s: " + ibaName);
            logger.trace("valueList: " + valueList);
        }

        AbstractValueView abstractvalueview = null;
        AttributeDefDefaultView attributedefdefaultview = null;
        Object aobj[] = (Object[]) ibaContainer.get(ibaName);
        Object aobj1[];
        List<Object> attributeViews = new ArrayList<Object>();
        Object attributesListObject;

        String value;
        AbstractValueView valueView;

        if (aobj != null) {
            abstractvalueview = (AbstractValueView) aobj[1];
            attributedefdefaultview = (AttributeDefDefaultView) aobj[0];
        }

        if (abstractvalueview == null) {
            attributedefdefaultview = getAttributeDefinition(ibaName);
        }

        if (attributedefdefaultview == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("attributedefdefaultview: " + attributedefdefaultview);
        }

        aobj1 = (Object[]) ibaContainer.get(attributedefdefaultview.getName());
        attributeViews.add(attributedefdefaultview);

        for (String string : valueList) {
            value = string;

            if (attributedefdefaultview instanceof UnitDefView) {
                value += " " + getDisplayUnits((UnitDefView) attributedefdefaultview, "SI");
            }

            valueView = internalCreateValue(attributedefdefaultview, value);

            if (logger.isDebugEnabled()) {
                logger.debug("Value: " + value + " written");
                logger.debug("abstractvalueview1: " + valueView);
            }

            if (aobj != null) {
                for (int j = 1; j < aobj1.length; j++) {
                    if (((AbstractValueView) aobj1[j]).compareTo(valueView) == 0) {
                        valueView = (AbstractValueView) aobj1[j];
                    }
                }
            }

            if (valueView == null) {
                return;
            }

            valueView.setState(AbstractValueView.NEW_STATE);
            attributeViews.add(valueView);
        }

        attributesListObject = attributeViews.toArray(new Object[attributeViews.size()]);
        ibaContainer.put(attributedefdefaultview.getName(), attributesListObject);
    }

    private String getDisplayUnits(UnitDefView unitdefview, String s) {

        if (logger.isTraceEnabled()) {
            logger.trace("getDisplayUnits(UnitDefView unitdefview, String s)");
            logger.trace("unitdefview: " + unitdefview);
            logger.trace("s: " + s);
        }

        QuantityOfMeasureDefaultView quantityofmeasuredefaultview = unitdefview.getQuantityOfMeasureDefaultView();
        String s1 = quantityofmeasuredefaultview.getBaseUnit();
        if (s != null) {
            String s2 = unitdefview.getDisplayUnitString(s);
            if (s2 == null) {
                s2 = quantityofmeasuredefaultview.getDisplayUnitString(s);
            }
            if (s2 == null) {
                s2 = quantityofmeasuredefaultview.getDefaultDisplayUnitString(s);
            }
            if (s2 != null) {
                s1 = s2;
            }
        }
        if (s1 == null) {
            return "";
        } else {
            return s1;
        }
    }

    public AttributeDefDefaultView getAttributeDefinition(String s) {

        if (logger.isTraceEnabled()) {
            logger.trace("getAttributeDefinition(String s)");
            logger.trace("s: " + s);
        }

        AttributeDefDefaultView attributedefdefaultview = null;
        try {
            attributedefdefaultview = IBADefinitionHelper.service.getAttributeDefDefaultViewByPath(s);
            if (attributedefdefaultview == null) {
                AbstractAttributeDefinizerView abstractattributedefinizerview = DefinitionLoader.getAttributeDefinition(s);
                if (abstractattributedefinizerview != null) {
                    attributedefdefaultview = IBADefinitionHelper.service.getAttributeDefDefaultView((AttributeDefNodeView) abstractattributedefinizerview);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace(System.out);
        }
        if (logger.isTraceEnabled()) {
            logger.trace(" - Will return: " + attributedefdefaultview);
        }
        return attributedefdefaultview;
    }

    public AbstractValueView internalCreateValue(AbstractAttributeDefinizerView abstractattributedefinizerview, String s) {

        if (logger.isTraceEnabled()) {
            logger.trace("internalCreateValue(AbstractAttributeDefinizerView abstractattributedefinizerview, String s)");
            logger.trace("abstractattributedefinizerview: " + abstractattributedefinizerview);
            logger.trace("s: " + s);
        }

        AbstractValueView abstractvalueview = null;
        if (abstractattributedefinizerview instanceof FloatDefView) {
            abstractvalueview = LoadValue.newFloatValue(abstractattributedefinizerview, s, null);
        } else if (abstractattributedefinizerview instanceof StringDefView) {
            abstractvalueview = LoadValue.newStringValue(abstractattributedefinizerview, s);
        } else if (abstractattributedefinizerview instanceof IntegerDefView) {
            abstractvalueview = LoadValue.newIntegerValue(abstractattributedefinizerview, s);
        } else if (abstractattributedefinizerview instanceof RatioDefView) {
            abstractvalueview = LoadValue.newRatioValue(abstractattributedefinizerview, s, null);
        } else if (abstractattributedefinizerview instanceof TimestampDefView) {
            abstractvalueview = LoadValue.newTimestampValue(abstractattributedefinizerview, s);
        } else if (abstractattributedefinizerview instanceof BooleanDefView) {
            abstractvalueview = LoadValue.newBooleanValue(abstractattributedefinizerview, s);
        } else if (abstractattributedefinizerview instanceof URLDefView) {
            abstractvalueview = LoadValue.newURLValue(abstractattributedefinizerview, s, null);
        } else if (abstractattributedefinizerview instanceof ReferenceDefView) {
            abstractvalueview = LoadValue.newReferenceValue(abstractattributedefinizerview, "ClassificationNode", s);
        } else if (abstractattributedefinizerview instanceof UnitDefView) {
            abstractvalueview = LoadValue.newUnitValue(abstractattributedefinizerview, s, null);
        }
        return abstractvalueview;
    }

    public String getIBAHierarchyID(String s) {

        if (logger.isTraceEnabled()) {
            logger.trace("getIBAHierarchyID(String s)");
            logger.trace("s: " + s);
        }

        String s1 = null;
        try {

            QuerySpec queryspec = new QuerySpec();

            int index = queryspec.appendClassList(AbstractAttributeDefinition.class, true);

            queryspec
                    .appendWhere(new SearchCondition(AbstractAttributeDefinition.class,
                                    "name", "=", s, true),
                            new int[]{index});

            QueryResult queryresult = persistenceManager.find((StatementSpec) queryspec);

            if (queryresult != null && queryresult.size() != 0) {
                AbstractAttributeDefinition abstractattributedefinition = (AbstractAttributeDefinition) queryresult.nextElement();
                s1 = abstractattributedefinition.getHierarchyID();
            }
        } catch (QueryException queryexception) {
            queryexception.printStackTrace(System.out);
        } catch (WTException wtexception) {
            wtexception.printStackTrace(System.out);
        }
        return s1;
    }

    public String getIBAName(String s) {

        if (logger.isTraceEnabled()) {
            logger.trace("getIBAName(String s)");
            logger.trace("s: " + s);
        }

        String s1 = null;
        try {

            QuerySpec queryspec = new QuerySpec();

            int index = queryspec.appendClassList(AbstractAttributeDefinition.class, true);

            queryspec
                    .appendWhere(new SearchCondition(AbstractAttributeDefinition.class,
                            "hierarchyID", SearchCondition.EQUAL, s, true), new int[]{index});

            QueryResult queryresult = PersistenceHelper.manager.find((StatementSpec) queryspec);
            if (queryresult != null && queryresult.size() != 0) {
                AbstractAttributeDefinition abstractattributedefinition = (AbstractAttributeDefinition) queryresult.nextElement();
                s1 = abstractattributedefinition.getName();
            }
        } catch (QueryException queryexception) {
            queryexception.printStackTrace(System.out);
        } catch (WTException wtexception) {
            wtexception.printStackTrace(System.out);
        }
        return s1;
    }

    /**
     * Clears the IBA with the specified name on the specified ibaHolder. The
     * method works on single valued and multivalued iba's. All the iba values
     * will be deleted. UpdateIbaPart and updateIbaHolder needs to be run to
     * commit the changes made by this method.
     *
     * @param ibaholder
     * @param ibaInternalName
     *
     * @return
     * @throws WTException
     * @throws WTPropertyVetoException
     * @throws RemoteException
     */

    /*
    public IBAHolder clearIBA(IBAHolder ibaholder, String ibaInternalName) throws WTException, WTPropertyVetoException, RemoteException {

        if (logger.isTraceEnabled()) {
            logger.trace("clearAllIBAs(IBAHolder ibaholder,String ibaInternalName)");
            logger.trace("ibaholder: " + LogHelper.toString(ibaholder));
            logger.trace("ibaInternalName: " + ibaInternalName);
        }

        ibaholder = IBAValueHelper.service.refreshAttributeContainer(ibaholder, null, sessionManager.getLocale(), null);
        DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) (new IBAValueDBService()).refreshAttributeConstraint(ibaholder, epmConstraintParam, null);
        AttributeDefDefaultView attributedefdefaultview;
        AbstractValueView abstractvalueview;
        Object aobj[];

        if (ibaholder instanceof WTPart) {
            defaultattributecontainer = suppressCSMConstraint(defaultattributecontainer);
        }

        aobj = (Object[]) ibaContainer.get(ibaInternalName);

        if (aobj != null && aobj.length > 1) {
            attributedefdefaultview = (AttributeDefDefaultView) aobj[0];

            for (int i = 1; i < aobj.length; i++) {
                abstractvalueview = (AbstractValueView) aobj[i];
                abstractvalueview.setState(2);
            }
        }

        return ibaholder;
    }
    */


    /**
     * Returns a FloatDefinition that can be used in method: setIBAFloatValue
     * for a given attribute
     *
     * @param attrName
     * @return
     * @throws WTException
     */
    public FloatDefinition getFloatDefinition(String attrName) throws WTException {
        if (logger.isTraceEnabled()) {
            logger.trace("getFloatDefinition(String attrName)");
            logger.trace("attrName: " + attrName);
        }

        FloatDefinition floatdfn = null;
        QuerySpec qs = new QuerySpec(FloatDefinition.class);
        qs.appendWhere(new SearchCondition(FloatDefinition.class,
                        FloatDefinition.NAME, SearchCondition.EQUAL, attrName),
                new int[]{0});
        QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
        while (qr.hasMoreElements()) {
            floatdfn = (FloatDefinition) qr.nextElement();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Returning floatdfn: " + floatdfn);
        }
        return floatdfn;
    }

    /**
     * Sets the FloatValue for a given attribute without iterating the IBAHolder
     *
     * @param ibaHolder
     * @param ibaName
     * @param newValue
     * @return
     */
    public boolean setIBAFloatValue(IBAHolder ibaHolder, String ibaName, FloatValue newValue) {
        if (logger.isTraceEnabled()) {
            logger.trace("setIBAFloatValue(IBAHolder ibaHolder, String ibaName, FloatValue newValue)");
            logger.trace("ibaHolder: " + LogHelper.toString(ibaHolder));
            logger.trace("ibaName: " + ibaName);
            logger.trace("newValue: " + newValue);
        }

        boolean success = false;

        try {

            QuerySpec qs = new QuerySpec(FloatDefinition.class);
            qs.appendWhere(new SearchCondition(FloatDefinition.class, FloatDefinition.NAME, SearchCondition.EQUAL, ibaName));
            QueryResult qr = PersistenceHelper.manager.find(qs);

            if (qr.hasMoreElements()) {

                FloatDefinition fd = (FloatDefinition) qr.nextElement();

                QuerySpec queryspec = new QuerySpec(FloatValue.class);
                queryspec.appendWhere(new SearchCondition(FloatValue.class,
                                "theIBAHolderReference.key",
                                SearchCondition.EQUAL,
                                ((Persistable) ibaHolder).getPersistInfo().getObjectIdentifier()),
                        new int[]{0});
                queryspec.appendAnd();
                queryspec.appendWhere(new SearchCondition(FloatValue.class,
                                "definitionReference.key",
                                SearchCondition.EQUAL,
                                fd.getPersistInfo().getObjectIdentifier()),
                        new int[]{0});

                QueryResult result = PersistenceHelper.manager.find(queryspec);

                if (result.hasMoreElements()) {
                    FloatValue fv = (FloatValue) result.nextElement();
                    fv.setValue(newValue.getValue());
                    PersistenceHelper.manager.save(fv);
                    success = true;

                } else {
                    PersistenceHelper.manager.save(newValue);
                    success = true;

                }
            }
        } catch (WTException | WTPropertyVetoException e) {
            logger.error("Failed to update attribute: " + ibaName + " with value: " + newValue + " for: " + LogHelper.toString(ibaHolder), e);
        }

        logger.trace("Returning success: " + success);
        return success;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "ibaholder=" + ibaholder + '}';
    }

    /**
     * Returns the attribute definition
     *
     * @param name
     * @return
     * @throws WTException
     */
    private AbstractAttributeDefinition getDefinition(String name) throws WTException {
        QuerySpec thisQuerySpec = new QuerySpec(AbstractAttributeDefinition.class);
        SearchCondition thisSearchCondition = new SearchCondition(AbstractAttributeDefinition.class, AbstractAttributeDefinition.NAME, SearchCondition.EQUAL, name);
        thisQuerySpec.appendWhere(thisSearchCondition);
        QueryResult thisQueryResult = PersistenceHelper.manager.find(thisQuerySpec);
        if (thisQueryResult.size() == 0) {
            throw new WTException("IBA definition '" + name + "' not found");
        }
        if (thisQueryResult.size() != 1) {
            throw new WTException("More than one IBA definition with name '" + name + "' found");
        }
        return (AbstractAttributeDefinition) thisQueryResult.nextElement();
    }

    /**
     * Returns oid for the provided object
     *
     * @param thisObject
     * @return
     * @throws WTException
     */
    private long getLongOid(Object thisObject) throws WTException {
        if (!(thisObject instanceof Persistable)) {
            throw new WTException("Cannot get identifier object is not persistent");
        }
        long id = ((Persistable) thisObject).getPersistInfo().getObjectIdentifier().getId();
        return id;
    }

    /**
     * Get the value for the provided ibaHolder and attribute(definition)
     *
     * @param ibaHolder
     * @param thisAbstractAttributeDefinition
     * @return
     * @throws WTException
     */
    private AbstractContextualValue getIBAValue(IBAHolder ibaHolder, AbstractAttributeDefinition thisAbstractAttributeDefinition) throws WTException {

        QuerySpec thisQuerySpec = new QuerySpec(AbstractContextualValue.class);
        long ibalongoid = getLongOid(ibaHolder);
        long definitionlongoid = getLongOid(thisAbstractAttributeDefinition);

        SearchCondition ibaHolderSearchCondition = new SearchCondition(AbstractContextualValue.class, "theIBAHolderReference.key.id", SearchCondition.EQUAL, ibalongoid);
        thisQuerySpec.appendWhere(ibaHolderSearchCondition);
        SearchCondition definitionSearchCondition = new SearchCondition(StringValue.class, "definitionReference.key.id", SearchCondition.EQUAL, definitionlongoid);
        thisQuerySpec.appendAnd();
        thisQuerySpec.appendWhere(definitionSearchCondition);
        QueryResult thisQueryResult = PersistenceHelper.manager.find(thisQuerySpec);
        if (thisQueryResult.size() == 0) {
            throw new WTException("IBA value '" + thisAbstractAttributeDefinition.getName() + "' not found");
        }
        if (thisQueryResult.size() != 1) {
            throw new WTException("More than one IBA value with name '" + thisAbstractAttributeDefinition.getName() + "' found");
        }
        return (AbstractContextualValue) thisQueryResult.nextElement();
    }

    /**
     * Get the attribute value as AbstractContextualValue
     *
     * @param ibaHolder
     * @param ibaname
     * @return
     * @throws WTException
     */
    private AbstractContextualValue getIBARaw(IBAHolder ibaHolder, String ibaname) throws WTException {

        AbstractAttributeDefinition thisAbstractAttributeDefinition = getDefinition(ibaname);
        AbstractContextualValue thisAbstractContextualValue = getIBAValue(ibaHolder, thisAbstractAttributeDefinition);

        return thisAbstractContextualValue;
    }

    /**
     * Delete the provided attribute for the provided IBAHolder
     *
     * @param ibaHolder
     * @param ibaname
     * @return
     * @throws WTException
     */
    public boolean deleteIBA(IBAHolder ibaHolder, String ibaname) throws WTException {

        AbstractContextualValue thisAbstractContextualValue = getIBARaw(ibaHolder, ibaname);
        if (thisAbstractContextualValue == null) {
            return false;
        }
        PersistenceHelper.manager.delete(thisAbstractContextualValue);
        return true;
    }

    @Deprecated
    public void persist(IBAHolder holder) throws WTException {
        persist();
    }

    public void persist() throws WTException {
        try {
            updateIBAPart(ibaholder);
            updateIBAHolder(ibaholder);
        } catch (WTPropertyVetoException | RemoteException ex) {
            throw new WTException(ex);
        }

    }

    /**
     * Clears the IBA with the specified name on the specified ibaHolder. The
     * method works on single valued and multivalued iba's. All the iba values
     * will be deleted. UpdateIbaPart and updateIbaHolder needs to be run to
     * commit the changes made by this method.
     *
     * @param ibaholder
     * @param ibaInternalName
     * @return
     * @throws WTException
     * @throws WTPropertyVetoException
     * @throws RemoteException
     */
    public IBAHolder clearIBA(IBAHolder ibaholder, String ibaInternalName) throws WTException, WTPropertyVetoException, RemoteException {
        if (logger.isTraceEnabled()) {
            logger.trace("clearAllIBAs(IBAHolder ibaholder,String ibaInternalName)");
            logger.trace("ibaholder: " + LogHelper.toString(ibaholder));
            logger.trace("ibaInternalName: " + ibaInternalName);
        }

        IBAValueHelper.service.refreshAttributeContainer(ibaholder, null, sessionManager.getLocale(), null);
        Object[] aobj = (Object[]) ibaContainer.get(ibaInternalName);

        if (aobj != null && aobj.length > 1) {
            for (int i = 1; i < aobj.length; i++) {
                AbstractValueView view = (AbstractValueView) aobj[i];
                view.setState(2);
            }
        }

        return ibaholder;
    }
}
