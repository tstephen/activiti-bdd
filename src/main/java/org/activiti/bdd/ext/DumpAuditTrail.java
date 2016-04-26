package org.activiti.bdd.ext;

import java.util.List;

import org.activiti.bdd.ActivitiSpec;
import org.activiti.bdd.ExternalAction;
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
        List<HistoricDetail> list = activitiRule
                .getHistoryService().createHistoricDetailQuery()
                .processInstanceId(spec.getProcessInstance().getId()).list();

        System.out.println("Audit trail: ");
        for (HistoricDetail hist : list) {
            System.out.println(String.format("  : %1$s", hist));
        }
    }
}