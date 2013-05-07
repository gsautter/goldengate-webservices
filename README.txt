The repository hosts the GoldenGATE Web Services infrastructure that allows
for deploying GoldenGATE text analysis logic in web services and make them
available to everyone.
The GoldenGATE Web Services infrastructure is developed by Guido Sautter on
behalf of Karlsruhe Institute of Technology (KIT) under the ViBRANT project
(EU Grant FP7/2007-2013, Virtual Biodiversity Research and Access Network
for Taxonomy).

Copyright (C) 2011-2013 ViBRANT (FP7/2007-2013, GA 261532), by G. Sautter

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program (LICENSE.txt); if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.



SYSTEM REQUIREMENTS

Java Runtime Environment 1.5 or higher, Sun/Oracle JRE recommended

Apache Tomcat 5.5 or higher (other servlet containers should work as well, but have not been tested yet)



DEPENDENCIES

GoldenGATE Web Services builds on other open source projects; the JAR files
these projects build into are included in the "lib" folder for convenience.
In addition, the Ant build script checks if these projects are present in your
workspace, and if so, uses the recent builds found in their "dist" folders.

- idaho-core (http://code.google.com/p/idaho-core/)

- idaho-extensions (http://code.google.com/p/idaho-extensions/)

- goldengate-editor (http://code.google.com/p/goldengate-editor/)



SETTING UP GoldenGATE Web Services

Build the WAR file using Ant, and then deploy GgWS.war to your Tomcat

Call http://localhost:8080/GgWS/ws to make Tomcat extract GgWS.war
(you might have to re-start Tomcat for it to recognize the WAR file)
(adjust server name and port if working remotely or Tomcat runs on a different port, respectively)

Tomcat's webapps folder should have a GgWS sub folder now, and it's time for some configuration:
To enable GoldenGATE Web Services to cache requests in the file system and download analysis logic from other servers, give the web application the permission to create and manipulate files and folders within its deployment folder and to establish outgoing network connections (there are two ways to achieve this):

    The simple, but very coarse way is to disable Tomcat's security manager altogether (not recommended)

    More finegrained way is to add the permission statement below to Tomcat's security configuration (recommended); the security configuration resides inside Tomcat's conf folder, which is located on the same level as the webapps folder; the actual configuration file to add the permission to is either catalina.policy directly in the conf folder, or 04webapps.policy in the conf/policy.d folder, whichever is present; if both files are present, either will do:

        grant codeBase "file:${catalina.base}/webapps/GgWS/WEB-INF/lib/-" {
        	permission java.net.SocketPermission "*.*", "connect";
        	permission java.io.FilePermission "WEB-INF/-", "read,write,delete,execute";
        }



CONFIGURING THE GoldenGATE WEB SERVICES RUNTIME BEHAVIOR

The following explanations refer to the "config.cnfg" file in the "webServiceData" folder inside the "WEB-INF" folder

The GgWS web applications loads its functionality (the actual services) from a GoldenGATE configuration on startup; there are three parameters that control which configuration is loaded, and from where: 

	GgConfigName: the name of the GoldenGATE configuration to load
	
	GgConfigPath: the path to load the configuration from; this can be (a) an absolute folder on the local computer (good for debugging), (b) a path relative to the servlet's data folder (starting with './'), or (c) a URL (starting with 'http://'); if this parameter is set, GgConfigHost is ignored, and all update mechanisms are disabled; if it is not set, the configuration is loaded from the 'Configurations' sub folder of the servlet's data folder, updating from the configured host (see below), and from any zipped configuration deposited in the 'Configurations' folder
	
	GgConfigHost: the URL to update configurations from; if this parameter is not set, updates via zip files still work

There are an additional three parameters that control the servlet's runtime behavior:

	allowUserInteraction: allow or disallow GoldenGATE web services to ask users for input / feedback, e.g. for double-checking the result of an automated classification or segmentation; if this parameter is not set, it defaults to 'true', enabling request for user feedback; if it is set to 'false' or 'no', user feedback is disabled, and all web services with interactivity setting 'always' become unavailable
	
	maxParallelRequests: the maximum number of requests to run in parallel; if this parameter is not set, it defaults to 0, meaning 'no limit'; limiting the number of parallel requests is especially helpful to control resource consumption for web services that are not interactive; when running mostly interactive web services, the limit should be higher, as most web services will likely spend a lot of time waiting on user input, so a low limit would result in minimum resource use, and possibly in a blocked pipeline
	
	maxParallelTokens: the maximum number of tokens to process in parallel; if this parameter is not set, it defaults to 0, meaning 'no limit'; this parameter has an effect similar to maxParallelRequest, but also factors in the amount of data associated with each request
	
If both maxParallelRequests and maxParallelTokens are 0, the servlet runs requests as they arrive, directly from memory, and only the result is cached on disc. If either of maxParallelRequests and maxParallelTokens is greater than 0, all requests are cached on disc first, and then handed to the scheduler that controls which request is processed when; the result is cached on disc as well. If the caching of requests on disc is desired, but requests should be processed right away anyway, simply set maxParallelRequests or maxParallelTokens to a large number, imposing a limit that will practically never prevent a request from being processed.

The web services included in the GoldenGATE configuration in this ZIP file are all non-interactive, so the allowUserInteraction parameters does not have any effect in this particular setup



UPDATING THE GoldenGATE CONFIGURATION

If the GgConfigHost parameter is set (see above), updates happen automatically

Otherwise, you can put a ZIP file containing the new version of the configuration into the "webServiceData/Configurations" inside the "WEB-INF" folder; it will be unzipped and used automatically on the next startup



IMPORTANT NOTE

The "index.html" file exists for test and demo purposes only, it is not meant to be a real user interface

The regular way of communication with GoldenGATE web services is from code via XML