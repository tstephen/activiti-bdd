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
package org.activiti.bdd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.activiti.engine.impl.test.JobTestHelper;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.toxos.activiti.assertion.ProcessAssert;

/**
 * Builds and runs process acceptance test cases using a fluent API.
 *
 * @author Tim Stephenson
 */
public class ActivitiSpec {

    private ActivitiRule activitiRule;

    private String specName;

    private ProcessInstance processInstance;

    private String messageName;

    private HashMap<String, Object> collectVars;

    private String processDefinitionKey;

    public ActivitiSpec(ActivitiRule activitiRule, String name) {
        this.activitiRule = activitiRule;
        this.specName = name;
        this.collectVars = new HashMap<String, Object>();
        writeBddPhrase("Instantiated specification for scenario %1$s", specName);
    }

    /**
     * @param preCondition
     *            Natural language definition of scenario pre-conditions.
     * @return
     */
    public ActivitiSpec given(String preCondition) {
        writeBddPhrase("%1$sGIVEN: %2$s", System.getProperty("line.separator"),
                preCondition);
        return this;
    }

    /**
     * Write a BDD phrase (Given, When or Then ...).
     * 
     * <p>
     * Default implementation writes to System.out.
     * 
     * @param phrase
     */
    protected void writeBddPhrase(String phrase) {
        System.out.println(phrase);
    }

    /**
     * Write a BDD phrase (Given, When or Then ...).
     * 
     * <p>
     * Default implementation writes to System.out.
     * 
     * @param phrase
     * @param args
     *            Substitution arguments for phrase.
     */
    protected void writeBddPhrase(String format, Object... args) {
        writeBddPhrase(String.format(format, args));
    }

    public Object getVar(String varName) {
        return collectVars.get(varName);
    }

    /**
     * @return The process instance started by the specification.
     */
    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    /**
     * Define the start event for the business process.
     * 
     * @param eventDescription
     *            'When' phase of scenario.
     * @param key
     *            Specifies the Process Definition to start.
     * @param collectVars
     *            Process variables to collect in the specification.
     * @param putVars
     *            Process variables to inject at process start.
     * @param tenantId
     *            Process tenant, may be null.
     * @return The updated specification.
     */
    public ActivitiSpec whenEventOccurs(String eventDescription, String key,
            Set<String> collectVars, Map<String, Object> putVars,
            String tenantId) {
        this.processDefinitionKey = key;

        HashMap<String, Object> vars = new HashMap<String, Object>();
        for (Entry<String, Object> entry : putVars.entrySet()) {
            vars.put(entry.getKey(), entry.getValue());
        }
        processInstance = activitiRule.getRuntimeService()
                .startProcessInstanceByKeyAndTenantId(processDefinitionKey,
                        vars, tenantId);
        assertNotNull(processInstance);
        assertNotNull(processInstance.getId());

        writeBddPhrase("WHEN: %1$s", eventDescription);
        return this;
    }

    /**
     * Define the message start event for the business process.
     * 
     * @param eventDescription
     *            'When' phase of scenario.
     * @param key
     *            Specifies the message name identifying the Process Definition
     *            to start.
     * @param messageResource
     *            Classpath resource to load and inject as process variable.
     * @param tenantId
     *            Process tenant, may be null.
     * @return The updated specification.
     */
    public ActivitiSpec whenMsgReceived(String eventDescription,
            String msgName, String messageResource, String tenantId) {
        this.messageName = msgName;

        InputStream is = null;
        Reader source = null;
        Scanner scanner = null;
        try {
            is = getClass().getResourceAsStream(messageResource);
            assertNotNull("Unable to load test resource: " + messageResource,
                    is);
            source = new InputStreamReader(is);
            scanner = new Scanner(source);
            String json = scanner.useDelimiter("\\A").next();

            HashMap<String, Object> vars = new HashMap<String, Object>();
            vars.put("messageName", adapt(msgName));
            vars.put(adapt(messageName), json);
            processInstance = activitiRule.getRuntimeService()
                    .startProcessInstanceByMessageAndTenantId(msgName,
                            vars, tenantId);
            assertNotNull(processInstance);
            assertNotNull(processInstance.getId());
        } finally {
            try {
                scanner.close();
            } catch (Exception e) {
                ;
            }
        }
        writeBddPhrase("WHEN: %1$s", eventDescription);
        return this;
    }

    /**
     * User Task resulting from the scenario specification.
     * 
     * <p>
     * Task will be asserted to exist, variables collected and/or updated and
     * then completed.
     * 
     * @param taskDefinitionKey
     *            Key (BPMN id) for user task.
     * @param collectVars
     *            Variable names to collect in the scenario.
     * @param putVars
     *            Variables to be injected into the process context.
     * @return The updated specification.
     */
    public ActivitiSpec thenUserTask(String taskDefinitionKey,
            Set<String> collectVars, Map<String, Object> putVars) {
        Task task = activitiRule.getTaskService().createTaskQuery()
                .singleResult();
        assertNotNull("Did not find the expected task with key "
                + taskDefinitionKey, task);
        assertEquals(taskDefinitionKey, task.getTaskDefinitionKey());

        for (String varName : collectVars) {
            collectVar(varName);
        }

        HashMap<String, Object> vars = new HashMap<String, Object>();
        for (Entry<String, Object> entry : putVars.entrySet()) {
            vars.put(entry.getKey(), entry.getValue());
        }

        activitiRule.getTaskService().complete(task.getId(), vars, false);
        for (Entry<String, Object> entry : putVars.entrySet()) {
            ProcessAssert.assertProcessVariableLatestValueEquals(
                    processInstance, entry.getKey(), entry.getValue());
        }
        writeBddPhrase("THEN: User Task '%1$s' is created and completed",
                taskDefinitionKey);
        return this;
    }

    /**
     * Execute an extension action for the scenario.
     * 
     * <p>
     * For example may be used to change state the process will subsequently
     * need or to perform custom assertions about the scenario.
     * 
     * @param action
     * @return The updated specification.
     * @throws Exception
     */
    public ActivitiSpec thenExtension(ExternalAction action) throws Exception {
        action.execute(this);
        writeBddPhrase("THEN: extension '%1$s' is run", action.getClass()
                .getName());
        return this;
    }

    /**
     * A scenario event allowing the process engine to execute for the specified
     * period.
     * 
     * @param maxMillisToWait
     *            Maximum milli-seconds to allow the engine to execute.
     * @return The updated specification.
     */
    public ActivitiSpec whenExecuteJobsForTime(int maxMillisToWait) {
        JobTestHelper.executeJobExecutorForTime(activitiRule, maxMillisToWait, 1);

        List<Job> jobs = activitiRule.getManagementService().createJobQuery()
                .list();
        writeBddPhrase("WHEN: executed jobs for %1$d, %2$d jobs remained",
                maxMillisToWait, jobs.size());

        return this;
    }

    /**
     * A scenario event allowing the process engine to execute for the specified
     * period.
     * 
     * @param timeout
     *            Maximum milli-seconds to allow the engine to execute.
     * @return The updated specification.
     */
    public ActivitiSpec whenExecuteAllJobs(int timeout) {
        JobTestHelper.waitForJobExecutorToProcessAllJobs(activitiRule
                .getProcessEngine().getProcessEngineConfiguration(),
                activitiRule.getManagementService(), timeout, 1);
        writeBddPhrase("WHEN: executed all jobs");
        return this;
    }

    /**
     * Advances process engine by the specified amount of time.
     * 
     * @param field
     *            One of the field constants in java.util.Calendar.
     * @param amount
     *            Amount to change field by.
     * @return The updated specification.
     * @see <a
     *      href="https://docs.oracle.com/javase/6/docs/api/java/util/Calendar.html">java.util.Calendar</a>
     */
    public ActivitiSpec whenProcessTimePassed(int field, int amount) {
        Calendar cal = activitiRule.getProcessEngine()
                .getProcessEngineConfiguration().getClock()
                .getCurrentCalendar();
        cal.add(field, amount);
        Date time = cal.getTime();
        writeBddPhrase("WHEN: process time advanced to : %1$s", time.toString());
        activitiRule.setCurrentTime(time);
        return this;
    }

    /**
     * Verify that the outcome of the scenario is that the process is complete.
     * 
     * @return The updated specification.
     */
    public ActivitiSpec thenProcessIsComplete() {
        ProcessAssert.assertProcessEnded(processInstance);
        writeBddPhrase("THEN: The process is complete");
        return this;
    }

    /**
     * Verify that the outcome of the scenario is that the process completed in
     * the specified BPMN event id.
     * 
     * @param endEventIds
     * @return The updated specification.
     */
    public ActivitiSpec assertProcessEndedAndInEndEvents(String... endEventIds) {
        ProcessAssert.assertProcessEndedAndInEndEvents(processInstance,
                endEventIds);
        return this;
    }

    /**
     * Verify that the outcome of the scenario is that the process completed in
     * the one and only BPMN event id.
     * 
     * @param endEventId
     * @return The updated specification.
     */
    public ActivitiSpec thenProcessEndedAndInExclusiveEndEvent(String endEventId) {
        ProcessAssert.assertProcessEndedAndInExclusiveEndEvent(processInstance,
                endEventId);
        return this;
    }

    /**
     * 
     * @param varName
     * @return The updated specification.
     */
    public ActivitiSpec collectVar(String varName) {
        Object var = activitiRule.getRuntimeService().getVariable(
                processInstance.getId(), varName);
        assertNotNull(var);
        collectVars.put(varName, var);
        return this;
    }

    private String adapt(String msgName) {
        return msgName.replace('.', '_');
    }

    /**
     * Creates an immutable pair to specify a process variable.
     * 
     * @param varName
     * @param varValue
     * @return an immutable pair representing a scenario variable.
     */
    public static ImmutablePair<String, Object> newPair(String varName,
            Object varValue) {
        return new ImmutablePair<String, Object>(varName, varValue);
    }

    /**
     * Convenience method to create a set of strings.
     * 
     * @param strings
     * @return
     */
    public static Set<String> buildSet(String... strings) {
        Set<String> set = new HashSet<String>();
        for (String s : strings) {
            set.add(s);
        }
        return set;
    }

    /**
     * Convenience method to create variable map.
     * 
     * @param immutablePair
     * @return
     */
    public static Map<String, Object> buildMap(
            @SuppressWarnings("unchecked") ImmutablePair<String, Object>... immutablePair) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (ImmutablePair<String, Object> pair : immutablePair) {
            map.put(pair.getKey(), pair.getValue());
        }
        return map;
    }

}
