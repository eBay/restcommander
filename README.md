[REST Commander](http://www.restcommander.com): Parallel Async HTTP Client as a Service ![Travis status](https://api.travis-ci.org/eBay/restcommander.png?branch=master)
===========

**Formerly known as REST Superman**. Please check detail instructions, screenshots, documentations, sample code and REST APIs  about REST Commander at [**www.restcommander.com**](http://www.restcommander.com) and its [**demo video**](http://www.youtube.com/watch?v=nMFhXxyE0EE). 

Fast Parallel Async HTTP/REST/SOAP client as a service to monitor and manage 10,000 web servers. Sends requests to 1000 servers with response aggregation in 10 seconds. or 10,000 servers in 50 seconds.

[What and Why](#a_whatAndWhy) | [Highlights](#a_highlights) | [Performance](#a_performance) | [Run Instructions](#a_runInstructions) 



REST Commander has been **[ranked  8th](http://www.restcommander.com/public/images/superman-8th-github-trending.png)** in Github trending in all languages on 01/21/2014 and 01/22/2014. It has been **[recommended](http://www.restcommander.com/public/images/oschina-recommend.png)** and listed in **[top 20](http://www.restcommander.com/public/images/superman-top-20-oschina.png)** hottest software of today in [oschina](http://www.oschina.net/p/restsuperman), the largest open source community in China. 

![Structure Overview](https://github.com/ebay/restcommander/raw/master/AgentMaster/public/images/workflow_v3.png)
<a name="a_whatAndWhy"></a>
###What is REST Commander and Why I need it?

Commander is [Postman](http://www.getpostman.com) at scale: a fast parallel async http client as a service with aggregated response and regular expression based string extraction. It is in Java (with AKKA and Play Framework).

So what can Commander do?  It speaks HTTP at scale, thus is powerful with [many use cases](http://www.restcommander.com/usecase.html). Here are some basic ones for automation on managing and monitoring tens of thousands web servers (See [Sample Code](http://www.restcommander.com/monitoring-sample.html))

Whenever comes to sending multiple HTTP requests in parallel, federated data aggregation or scalable task executions on HTTP, **Think Commander First**.

* **Monitor HTTP web servers**:  are you a company who have 50-5,000 web servers (e.g. tomcat, nginx, etc... ) running;  and want to check every minute which servers are slow or misconfigured? Commander can get this done for you in an hour.
* **Config push to HTTP web servers**: If your servers use REST/SOAP APIs to update its config and you want to enforce server-specific or uniform config on demand or with auto-remediation. Commander is your perfect choice.  
* **HTTP web server management work flows combining the above 2**: e.g., discover unhealthy webservers and then conduct operations (restart, config push) to them.
<a name="a_highlights"></a>

###Highlights

* **Scalable and Fast**: Utilizes AKKA and Async HTTP Client to maximize concurrency; Sends and aggregates responses from 10,000+ Servers within 1 minute.
* **Powerful**: Sends uniform or node-specific requests with near real-time response analysis or config pushes. Request level concurrency control.
* **Generic**: Generic HTTP request. Generic response aggregation with user-defined regular expression matching. Generic variable replacement in request templates for node specific requests.
* **Ready to Use**: Agility. Zero installation required. Changing requests and target servers with breeze. No database setup. Run locally in a single click.
* **User Friendly**: Build in Java with Play Framework, Bootstrap and its Application Wizard, Commander enables sending requests in both easy-to-use web UI wizards and powerful REST APIs. Define ad hoc requests, target servers and the regex aggregation rule in a single REST call.
* **Agent-less Monitoring**: Quickly check any HTTP results from an ad hoc list of servers with generic response aggregation by regular expression matching. FAST: No dependency or setup required. 
* **Config Pushes**: Push uniform config or node-specific configs to HTTP end points, as long as there are HTTP (REST/SOAP) APIs to perform.
* **N Requests to 1 Target**: Concurrently fire a large number of different requests to a single target server. E.g. look up thousands of jobs status in a server. Concurrency control to accommodate server capacity.
* **IT Orchestration**: Scalable multi-step HTTP work flows to thousands of HTTP endpoints.
* **Discover Outliers**: Discover misconfigured servers from thousands of servers with http APIs in no time.
* **Remediation Automation**: If your config change requests are idempotent, Commander can easily ensure correct config by scheduled config pushes.

Commander is powerful to send (1) the same request to different servers; (2) different requests to different servers; (3) different requests to the same server. Why we need them? Check out these [live examples](http://www.restcommander.com/usecase.html) on (1) monitor websites; (2) poll job status (3) call the same weather WSDL web service with different zip codes.

<a name="a_performance"></a>
###Performance (SLA)
* Measured from Commander running on a **single off-the-shelf server**.
* 1000 servers requests and all responses aggregated in 7 seconds 
* 10,000 servers requests and all responses aggregated in 48 seconds
* 20,000 servers requests and all responses aggregated in 70 seconds
* 20,000 is far less than the maximum scale we tested and it never crashed. We have not been able to find the scalability limit. :-) 

###[Run Instructions](http://www.restsuperman.com/get-started.html#a_zero_installation)<a name="a_runInstructions"></a>

#### Directly Under Windows/Linux With Zero Installation: 
* Assuming have Java (JDK or most time just JRE) pre-installed.

##### WINDOWS
* double click: start_application_win.bat . Just close the window to shutdown the server. 
* Then open browser: [localhost:9000](http://localhost:9000/)
* After shutdown the application: double click: clean_application_pid_after_run_win.bat

##### LINUX or MAC
* Note that for Linux/Mac user: need to chmod +x for play-1.2.4/play
* Run start_application_linux.sh

#### Run/Debug With Eclipse:
* Clone project in Git from: https://github.com/eBay/restcommander
* Extract to a folder, e.g., S:\GitSources\AgentMaster\AgentMaster. In command line run: S:\GitSources\AgentMaster\AgentMaster>play-1.2.4\play eclipsify AgentMaster
	* Note that for Linux/Mac user: need to chmod +x for play-1.2.4/play
* Import existing project in Eclipse: import the AgentMaster folder.
* Compile errors? Try rebuild and clean: (menu: Project->Clean->Clean all projects
* Run application: under "eclipse" folder: AgentMaster.launch : run as AgentMaster
* Then open browser: [localhost:9000](http://localhost:9000/)


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



### About REST Commander

####Motivation: 

REST and SOAP API (HTTP GET/POST/PUT/DELETE) has become the dominant approach in current platform and services. However, efficient REST / SOAP calls to multiple servers in parallel with server-specific requests and aggregated response analysis are still challenging.

####Problem Statement:

Design and implement a user friendly and generic HTTP client able to conduct efficient HTTP calls to a large amount of servers in parallel with uniform or server-specific requests and aggregated response analysis. 


####Impact


* Improve HTTP/SOAP/REST call efficiency by 100%-5000% with 3-15000 target nodes in parallel, compared to single server POSTMAN or sequential executed none-generic shell scripts.
* Enable any uniform or server-specific REST (GET/POST/PUT/DELETE) calls to servers for periodical monitoring and configuration pushes in a simple UI with fast and reliable with responses aggregation. Automate tens of thousands of server's management and software pool management.
* **Innovation**: After thoroughly reviewing related work of Postman, JMeter, Gatling, Apache Bench,  Typhoeus and many other publication or tools, we are not aware of any existing ones are able to achieve the same speed, scale and functionality of generic response aggregation.



[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/eBay/restcommander/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

