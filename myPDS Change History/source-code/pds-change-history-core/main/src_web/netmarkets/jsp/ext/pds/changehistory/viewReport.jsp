<%@page language="java" session="false" import="ext.pds.changehistory.report.ReportProcessor" %>
<%@page import="java.net.URL"%>
<%
URL url = ReportProcessor.getInstance().getReportURL(request.getParameter("oid"));
out.clearBuffer();
if (url != null) {         
    out.write(url.toString()); 
}
out.flush();
%>