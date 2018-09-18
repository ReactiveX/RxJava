RxJava is still a work in progress and has a long list of work documented in the [[Issues|https://github.com/ReactiveX/RxJava/issues]].


If you wish to contribute we would ask that you:
- read [[Rx Design Guidelines|http://blogs.msdn.com/b/rxteam/archive/2010/10/28/rx-design-guidelines.aspx]]
- review existing code and comply with existing patterns and idioms
- include unit tests
- stick to Rx contracts as defined by the Rx.Net implementation when porting operators (each issue attempts to reference the correct documentation from MSDN)

Information about licensing can be found at: [[CONTRIBUTING|https://github.com/ReactiveX/RxJava/blob/1.x/CONTRIBUTING.md]].

## How to import the project into Eclipse
Two options below:

###Import as Eclipse project 

    ./gradlew eclipse

In Eclipse 
* choose File - Import - General - Existing Projects into Workspace
* Browse to RxJava folder
* click Finish.
* Right click on the project in Package Explorer, select Properties - Java Compiler - Errors/Warnings - click Enable project specific settings.
* Still in Errors/Warnings, go to Deprecated and restricted API and set Forbidden reference (access-rules) to Warning.

###Import as Gradle project

You need the Gradle plugin for Eclipse installed.

In Eclipse 
* choose File - Import - Gradle - Gradle Project. 
* Browse to RxJava folder
* click Build Model
* select the project
* click Finish






