<%@ taglib prefix="jca" uri="http://www.ptc.com/windchill/taglib/components"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ include file="/netmarkets/jsp/components/beginWizard.jspf"%>
<%@ include file="/netmarkets/jsp/components/includeWizBean.jspf"%>
<script type="text/javascript">
    function doClose() {
        PTC.wizard.unloadWizard();
        PTC.wizard.close();
    };
</script>
<jca:wizard title="Generate Report" buttonList="WizardButtonClose">
	<jca:wizardStep action="reportWizardStep" type="pds-ch" />
</jca:wizard>

<%@include file="/netmarkets/jsp/util/end.jspf"%>