package org.ei.drishti.service.reporting;

import org.ei.drishti.common.AllConstants;
import org.ei.drishti.common.domain.Caste;
import org.ei.drishti.common.domain.EconomicStatus;
import org.ei.drishti.common.domain.Indicator;
import org.ei.drishti.common.domain.ReportingData;
import org.ei.drishti.domain.EligibleCouple;
import org.ei.drishti.domain.Location;
import org.ei.drishti.repository.AllEligibleCouples;
import org.ei.drishti.util.SafeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.ei.drishti.common.AllConstants.CommonFormFields.ID;
import static org.ei.drishti.common.AllConstants.FamilyPlanningFormFields.*;

@Service
public class ECReportingService {
    private ReportingService service;
    private AllEligibleCouples allEligibleCouples;

    @Autowired
    public ECReportingService(ReportingService service, AllEligibleCouples allEligibleCouples) {
        this.service = service;
        this.allEligibleCouples = allEligibleCouples;
    }

    public void registerEC(SafeMap reportData) {
        EligibleCouple couple = allEligibleCouples.findByCaseId(reportData.get(ID));
        reportFPMethod(reportData, couple, Indicator.from(reportData.get(CURRENT_FP_METHOD_FIELD_NAME)));
        reportOCPCasteBasedIndicators(reportData, couple);
        reportFemaleSterilizationEconomicStatusBasedIndicators(reportData, couple);
    }

    public void fpChange(SafeMap reportData) {
        EligibleCouple couple = allEligibleCouples.findByCaseId(reportData.get(ID));
        reportFPMethod(reportData, couple, Indicator.from(reportData.get(CURRENT_FP_METHOD_FIELD_NAME)));
        reportOCPCasteBasedIndicators(reportData, couple);
        reportFemaleSterilizationEconomicStatusBasedIndicators(reportData, couple);
    }

    private void reportOCPCasteBasedIndicators(SafeMap reportData, EligibleCouple ec) {
        if (OCP_FP_METHOD_VALUE.equalsIgnoreCase(reportData.get(CURRENT_FP_METHOD_FIELD_NAME))) {
            reportFPMethod(reportData, ec, Caste.from(reportData.get(AllConstants.ECRegistrationFields.CASTE)).indicator());
        }
    }

    private void reportFemaleSterilizationEconomicStatusBasedIndicators(SafeMap reportData, EligibleCouple couple) {
        if (FEMALE_STERILIZATION_FP_METHOD_VALUE.equalsIgnoreCase(reportData.get(CURRENT_FP_METHOD_FIELD_NAME))) {
            reportFPMethod(reportData, couple, EconomicStatus.from(reportData.get(AllConstants.ECRegistrationFields.ECONOMIC_STATUS)).indicator());
        }
    }

    private void reportFPMethod(SafeMap reportData, EligibleCouple ec, Indicator indicator) {
        if (indicator == null) {
            return;
        }

        ReportingData serviceProvidedData = ReportingData.serviceProvidedData(ec.anmIdentifier(), ec.ecNumber(),
                indicator, reportData.get(FP_METHOD_CHANGE_DATE_FIELD_NAME), new Location(ec.village(), ec.subCenter(), ec.phc()));
        ReportingData anmReportData = ReportingData.anmReportData(ec.anmIdentifier(), reportData.get(ID),
                indicator, reportData.get(FP_METHOD_CHANGE_DATE_FIELD_NAME));

        service.sendReportData(serviceProvidedData);
        service.sendReportData(anmReportData);
    }
}
