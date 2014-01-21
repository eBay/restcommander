[REST Superman](http://www.restsuperman.com): Parallel Async HTTP Client as a Service
===========
Please check details about REST Superman at [www.restsuperman.com](http://www.restsuperman.com) and its [introduction video](http://www.youtube.com/watch?v=nMFhXxyE0EE).

![Structure Overview](https://github.com/ebay/restsuperman/raw/master/AgentMaster/public/images/workflow_v3.png)

###What is REST Superman and Why I need it?

Superman is [Postman](http://www.getpostman.com) at scale: a fast parallel async http client as a service with aggregated response and regular expression based string extraction.

So what can Superman do?  It speaks HTTP in scale, thus is powerful with many use cases. Here are some basic ones for automation on managing and monitoring tens of thousands web servers:

* **Monitor HTTP web servers**:  are you a company who have 50-5,000 web servers (e.g. tomcat, nginx, etc... ) running;  and want to check every minute which servers are slow or misconfigured? Superman can get this done for you in an hour.
* **Config push to HTTP web servers**: If your servers use REST/SOAP APIs to update its config and you want to enforce server-specific or uniform config on demand or with auto-remediation. Superman is your perfect choice.  
* **HTTP web server management work flows combining the above 2**: e.g., discover unhealthy webservers and then conduct operations (restart, config push) to them.


###Highlights

* **Scalable and Fast**: Utilizes AKKA and Async HTTP Client to maximize concurrency; Sends and aggregates responses from 10,000+ Servers within 1 minute.
* **Powerful**: Sends uniform or node-specific requests with near real-time response analysis or config pushes. Request level concurrency control.
* **Generic**: Generic HTTP request. Generic response aggregation with user-defined regular expression matching. Generic variable replacement in request templates for node specific requests.
* **Ready to Use**: Agility. Zero installation required. Changing requests and target servers with breeze. No database setup. Run locally in a single click.
* **User Friendly**: Build in Java with Play Framework, Bootstrap and its Application Wizard, Superman enables sending requests in both easy-to-use web UI wizards and powerful REST APIs.
* **Agent-less Monitoring**: Quickly check any HTTP results from an ad hoc list of servers with generic response aggregation by regular expression matching. FAST: No dependency or setup required. 
* **Config Pushes**: Push uniform config or node-specific configs to HTTP end points, as long as there are HTTP (REST/SOAP) APIs to perform.
* **N Requests to 1 Target**: Concurrently fire a large number of different requests to a single target server. E.g. look up thousands of jobs status in a server. Concurrency control to accommodate server capacity.
* **IT Orchestration**: Scalable multi-step HTTP work flows to thousands of HTTP endpoints.
* **Discover Outliers**: Discover misconfigured servers from thousands of servers with http APIs in no time.
* **Remediation Automation**: If your config change requests are idepotent, Superman can easily ensure correct config by scheduled config pushes.


###Run Instructions

#### Directly Under Windows/Linux With Zero Installation: 
* Assuming have Java (JDK or most time just JRE) pre-installed.

##### WINDOWS
* double click: start_application_win.bat . Just close the window to shutdown the server. 
* Then open brower: http://localhost:9000/
* After shutdown the application: double click: clean_application_pid_after_run_win.bat

##### LINUX or MAC
* Note that for Linux/Mac user: need to chmod +x for play-1.2.4/play
* Run start_application_linux.sh

#### Run/Debug With Eclipse:
* Clone project in Git from: https://github.com/eBay/restsuperman
* Extract to a folder, e.g., S:\GitSources\AgentMaster\AgentMaster. In command line run: S:\GitSources\AgentMaster\AgentMaster>play-1.2.4\play eclipsify AgentMaster
	* Note that for Linux/Mac user: need to chmod +x for play-1.2.4/play
* Import existing project in Eclipse: import the AgentMaster folder.
* Compile errors? Try rebuild and clean: (menu: Project->Clean->Clean all projects
* Run application: under "eclipse" folder: AgentMaster.launch : run as AgentMaster
* Then open brower: http://localhost:9000/


### Settings
Key files are under *conf* folder
* agentcommand.conf : defines commands
* nodegroup.conf : defines the node list: ad hoc; from ODB; from Statehub.
* aggregation.conf : defines  aggregation: using the default one to parse number out.
* application.conf : play settings
* actorconfig.conf : Akka settings
* routes : MVC settings as dispatcher

### Troubleshooting
* Under windows: shutdown in the middle of sending requests or many requests: No buffer space available (maximum connections reached ?)
	* http://rwatsh.blogspot.com/2012/04/resolution-for-no-buffer-space.html
	* The resolution is to open the registry editor and locate the registry subkey: HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters and add a new entry as shown below:
		* Value Name: MaxUserPort
		* Value Type: DWORD
		* Value data: 65534



### About REST-Superman

####Motivation: 

REST API (HTTP GET/POST/PUT/DELETE) has become the dominant approach in current platform and services. However, efficient REST calls to multiple nodes in parallel with node-specific requests and aggregated response analysis are still challenging.

####Problem Statement:

Design and implement a user friendly and generic REST client able to conduct efficient REST calls to a large amount of nodes in parallel with uniform or node-specific requests and aggregated response analysis. 

####SLA Objective: 
* <5 minutes for REST requests to up to 10,000 nodes in parallel.
* <1 minute for REST requests to up to 200 nodes in parallel.

####Current SLA: 
* Measured from superman running a single server
* 1000 servers request and all responses obtained in 7 seconds 
* 20,000 servers request and all responses obtained in 70 seconds


####Impact


* Improve REST call efficiency by 100%-5000% with 3-15000 target nodes in parallel, compared to single node POSTMAN or sequential executed none-generic shell scripts.
* Enable any uniform or node-specific REST (GET/POST/PUT/DELETE) calls to nodes for periodical monitoring and configuration pushes in a simple UI with fast and reliable with responses aggregation. Automate tens of thousand of server's management and software pool management.
* Innovation: After reviewing related work, we are not aware of any existing publication or open source software did the same thing. 

