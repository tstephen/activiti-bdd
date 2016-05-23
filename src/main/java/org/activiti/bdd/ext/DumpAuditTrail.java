package org.activiti.bdd.ext;

import java.util.List;

import org.activiti.bdd.ActivitiSpec;
import org.activiti.bdd.ExternalAction;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.test.ActivitiRule;

public class DumpAuditTrail implements ExternalAction {

    private final ActivitiRule activitiRule;

    /**
     * @param activitiRule
     */
    public DumpAuditTrail(ActivitiRule activitiRule) {
        this.activitiRule = activitiRule;
    }

    public void execute(ActivitiSpec spec) throws Exception {
        System.out.println("Audit trail: ");

        List<HistoricActivityInstance> activities = activitiRule
                .getHistoryService().createHistoricActivityInstanceQuery()
                .processInstanceId(spec.getProcessInstance().getId()).list();

        for (HistoricActivityInstance hist : activities) {
            System.out.println(String.format("  : %1$s", hist));
        }

        System.out.println("Final data: ");
        List<HistoricDetail> details = activitiRule
                .getHistoryService().createHistoricDetailQuery()
                .processInstanceId(spec.getProcessInstance().getId()).list();

        for (HistoricDetail hist : details) {
            System.out.println(String.format("  : %1$s", hist));
        }
    }
}