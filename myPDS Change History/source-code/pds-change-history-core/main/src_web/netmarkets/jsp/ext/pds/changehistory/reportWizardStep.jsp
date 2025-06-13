<p id="initialStatus">The BOM Report is being generated and will soon be available.</p>
<br/>
<p id="loading"><img src="netmarkets/images/lwc_saving.gif" /></p>
<p id="fileStatus" style="visibility:hidden;">The Change report has been generated and is now available for <a id="downloadLink" target="_blank">download</a>.</p>

<script type="text/javascript">


	var loadingPara = document.getElementById("loading");
	var fileStatusPara = document.getElementById("fileStatus");
	var initialStatusPara = document.getElementById("initialStatus");
	var oid = "${param.oid}";	
	var url = "netmarkets/jsp/ext/pds/changehistory/viewReport.jsp";
	var options = {
		asynchronous: true,
		parameters: "oid=" + oid + "&reqId=" + new Date().getTime(),
		onSuccess: function(response) {
			loadingPara.style.display = "none";
			var downloadLinkAnchor = document.getElementById("downloadLink");
			downloadLinkAnchor.href = response.responseText;
			fileStatusPara.style.visibility = "visible";
			initialStatusPara.style.visibility = "hidden";
		},
		onFailure: function(response) {
			loadingPara.style.display = "none";
			var detailsContent = response.responseText;
			function openDetailsWindow(event) {
				event.preventDefault();
				var detailsWin = window.open();
				detailsWin.document.open();
				detailsWin.document.write(detailsContent);
				detailsWin.document.close();
			}
			
			var message = document.createElement("p");
			message.innerHTML = "Failure to generate Report: " + response.statusText;
			var link = document.createElement("a");
			link.href = "#";
			link.target="_blank";
			link.onclick=openDetailsWindow;
			link.innerHTML = "Click for details";
			message.appendChild(link);
			
			fileStatusPara.innerHTML = "";
			fileStatusPara.appendChild(message);
			fileStatusPara.appendChild(link);
			fileStatusPara.style.visibility = "visible";
		}
	};
	
	requestHandler.doRequest(url, options);

</script>