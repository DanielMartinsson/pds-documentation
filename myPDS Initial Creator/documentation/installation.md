% class="wikigeneratedid" %)
 
= Preparation =
Before the feature can be installed there are few steps needed to prepare the installation. 
1. Copy the deploy package onto the target system
1. Unpack the package to a directory outside of the Windchill home folder (WT_HOME)
This directory will be referred to as the ##DEPLOY_ROOT## and in the screenshot below this is the folder ##pds-initial-creator-1.0.0_wc13.0 ##but this may vary depending on the version being installed.
\\[[image:1749527145997-126.png]]
 
1. Open the file ##DEPLOY_ROOT/config/deployment.properties## in text editor and update the following properties found at the bottom of the file.(((
 
|=(% style="width: 217px;" %)Property|=(% style="width: 909px;" %)Value
|(% style="width:217px" %)pdmlink.auth.username|(% style="width:909px" %)The username of the user the installer will run as
|(% style="width:217px" %)pdmlink.auth.password|(% style="width:909px" %)The password of the user the installer will run as
|(% style="width:217px" %)windchill.service.name|(% style="width:909px" %)This is the name of the Windows Service for Windchill.
\\{{info}}If you are on Linux or are not running Windchill as a service then the value should be set to an empty string.{{/info}}
 
All other properties apart from the ones mentioned above can be left as is.
)))
= Installation =
Follow the steps described below to install the feature.
1. Open a Windchill shell and navigate to the ##DEPLOY_ROOT## folder 
1. If you are running on Linux you have to mark the deployment scrip as executable. Run the following command to do this 
##chmod +x bin/pds-deployment##
\\Listing the files in the bin folder should now show the file as executable
\\[[image:1749557016917-479.png||data-xwiki-image-style-border="true"]]
 
1. (((
Run the appropriate command depending on the target operating system
Windows: ##bin\pds-deployment.bat##
Linux:## bin/pds-deployment.sh##
\\[[image:1749557549536-996.png]]
{{warning}}
It is important that the deployment script is invoked from the DEPLOY_ROOT folder and not from the bin folder itself. 
If it is not run from the root folder it will not be able to find a number of required resources.
{{/warning}}
)))
(% class="wikigeneratedid" id="H" %)
