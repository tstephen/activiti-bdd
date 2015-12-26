/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Activiti Behaviour Driven Development (BDD) library
 * Copyright 2015 Tim Stephenson
 * 
 *******************************************************************************/
package org.activiti.bdd.examples;

import org.activiti.bdd.ActivitiSpec;
import org.activiti.engine.test.ActivitiRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Simplest possible example specification using Activiti BDD.
 *
 * @author Tim Stephenson
 */
public class Example1Test {

    private static final String EXAMPLE1_KEY = "Example1";

    @Rule
    public ActivitiRule activitiRule = new ActivitiRule("test-activiti.cfg.xml");

    @SuppressWarnings("unchecked")
    @Test
    @org.activiti.engine.test.Deployment(resources = { "processes/Example1.bpmn" })
    public void testExample1() throws Exception {
        new ActivitiSpec(activitiRule, "testExample1")
                .given("No particular pre-conditions")
                .whenEventOccurs("The Example1 process is started",
                        EXAMPLE1_KEY, ActivitiSpec.buildSet(),
                        ActivitiSpec.buildMap(), null)
                .thenUserTask("doSomething", ActivitiSpec.buildSet(),
                        ActivitiSpec.buildMap())
                .thenProcessIsComplete();

    }

}