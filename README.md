Activiti BDD
=============================

A library to support Behaviour Driven Development of business processes with the Activiti BPMN engine. 

Installation
=============================

Plain installation
---------------------------------

At this stage it is necessary to build the library from source: 

1. Clone source: 

   ```
   git clone https://github.com/tstephen/activiti-bdd.git
   ```

2. Build and install in local maven repo: 

    ```
    mvn clean install
    ```

Maven installation
---------------------------------

Would be a good addition!

Usage
=============================

1. Behaviour specifications are written in the usual text format, for example: 

    > GIVEN: No particular pre-conditions
    > WHEN: The Example1 process is started
    > THEN: User Task 'doSomething' is created and completed
    > THEN: The process is complete
   
2. A fluent Java API enables a highly readable translation of the spec to an executable form.

    ```java
    new ActivitiSpec(activitiRule, "testExample1")
        .given("No particular pre-conditions")
        .whenEventOccurs("The Example1 process is started",
                EXAMPLE1_KEY, 
                ActivitiSpec.buildSet(),
                ActivitiSpec.buildMap(), 
                null)
        .thenUserTask("doSomething", 
                ActivitiSpec.buildSet(),
                ActivitiSpec.buildMap())
        .thenProcessIsComplete();
    ```
    
3. Embedding within a JUnit test provides a simple way to integrate the specification into continuous integration and deployment environments. See Example1.java for the complete class. 