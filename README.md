# tradingDataCenter
Project Code Description for IT60 Research Study by Yongqiang Wu

##Project Overview

In this research study, there are two problems that have been studied: 
* the static VM placement problem 
* the dynamic VM placement. 

For the first problem a GA and an SA are proposed to solve the problem. Accordingly there are two java projects for the first problem research. For the dynamic VM placement, the trading method is used to tackle this dynamic VM placement.
All the source codes are written in Java. The projects were created by Eclipse Indigo version. Eclipse is a full-featured free open-sourced Java developing environment.

For the convenience of data analysis, all the outputs are stored in the text files. It is recommended using the ‘grep’ text search tool to retrieve the data interesting to you.
In LogPrint.java the log output directory is default as blow. It is necessary to change it to suit your file system on your computer.

	private static final String C_USERS_PUBLIC = "C:\\users\\public\\";
  
Here is the project source code structure

|Project Name|	Description |	Test entry class|
|---|---|---|
|Common|	Common code for the other three projects|	No|
|SA|	Code for the SMC paper| SaPlacement.main() run as application|
|  |VM placement for server + network energy|TestNetworkCost.test() run as junit test	|
|GA|	Code for the ICONIP paper|	DCEnergy.main() run as application
|  Trading | Code for dynamic trading|	TestTradingDynamicPlacement.testFFDPlacment()|
|  | |TestTradingDynamicPlacement.testTradingPlacment()|
| | |TestTradingDynamicPlacement.testTradingPlacmentAccrossGrps()|
| | |all are run as Junit test|

