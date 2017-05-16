package org.opensrp.register.mcare.report.mis1;

import org.opensrp.register.mcare.domain.Members;

import java.util.HashMap;
import java.util.Map;

import static org.opensrp.register.mcare.domain.Members.EligibleCoupleVisitKeyValue.*;

public class EligibleCoupleFollowUP {
    private Map<String, String> followUp;

    public Map<String, String> getFollowUp() {
        return followUp;
    }

    public static class EligibleCoupleFollowUpBuilder {
        private Map<String, String> followUp;

        public EligibleCoupleFollowUpBuilder() {
            followUp = new HashMap<>();
        }

        public EligibleCoupleFollowUpBuilder clientVersion(long clientVersion) {
            followUp.put(Members.CLIENT_VERSION_KEY, String.valueOf(clientVersion));
            return this;
        }
        public EligibleCoupleFollowUpBuilder pregnant(String pregnantStatus) {
            followUp.put(Key.PREGNANT_STATUS, pregnantStatus);
            return this;
        }

        public EligibleCoupleFollowUpBuilder tt_dose(String ttDoses) {
            followUp.put(Key.TT_DOSE, ttDoses);
            return this;
        }

        public EligibleCoupleFollowUP build() {
            EligibleCoupleFollowUP eligibleCoupleFollowUP = new EligibleCoupleFollowUP(this);
            this.followUp.clear();
            return eligibleCoupleFollowUP;
        }
    }

    private EligibleCoupleFollowUP(EligibleCoupleFollowUpBuilder eligibleCoupleFollowUpBuilder) {
        this.followUp = new HashMap<>();
        this.followUp.putAll(eligibleCoupleFollowUpBuilder.followUp);
    }

}
