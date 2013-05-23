package org.ei.drishti.service;

import org.ei.drishti.contract.*;
import org.ei.drishti.domain.Child;
import org.ei.drishti.domain.Mother;
import org.ei.drishti.dto.BeneficiaryType;
import org.ei.drishti.repository.AllChildren;
import org.ei.drishti.repository.AllEligibleCouples;
import org.ei.drishti.repository.AllMothers;
import org.ei.drishti.service.reporting.ChildReportingService;
import org.ei.drishti.service.reporting.MotherReportingService;
import org.ei.drishti.service.scheduling.ChildSchedulesService;
import org.ei.drishti.util.SafeMap;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.testing.utils.BaseUnitTest;
import org.motechproject.util.DateUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.ei.drishti.dto.AlertStatus.normal;
import static org.ei.drishti.dto.BeneficiaryType.child;
import static org.ei.drishti.dto.BeneficiaryType.mother;
import static org.ei.drishti.util.EasyMap.create;
import static org.ei.drishti.util.EasyMap.mapOf;
import static org.ei.drishti.util.Matcher.objectWithSameFieldsAs;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PNCServiceTest extends BaseUnitTest {
    @Mock
    private ActionService actionService;
    @Mock
    private ChildSchedulesService childSchedulesService;
    @Mock
    private AllEligibleCouples allEligibleCouples;
    @Mock
    private AllMothers mothers;
    @Mock
    private AllChildren children;
    @Mock
    private MotherReportingService motherReportingService;
    @Mock
    private ChildReportingService childReportingService;
    @Mock
    private PNCSchedulesService pncSchedulesService;
    @Mock
    private ECService ecService;

    private PNCService service;
    private Map<String, Map<String, String>> EXTRA_DATA = create("details", mapOf("someKey", "someValue")).put("reporting", mapOf("someKey", "someValue")).map();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service = new PNCService(ecService, actionService, childSchedulesService, pncSchedulesService, allEligibleCouples, mothers, children, motherReportingService, childReportingService);
    }

    @Test
    public void shouldAddAlertsForVaccinationsForChildren() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        when(mothers.findByCaseId("MOTHER-CASE-1")).thenReturn(new Mother("MOTHER-CASE-1", "EC-CASE-1", "TC1"));

        service.registerChild(new ChildInformation("Case X", "MOTHER-CASE-1", "ANM X", "Child 1", "female", LocalDate.now().toString(), "", "4", "yes", EXTRA_DATA));

        verify(actionService).alertForBeneficiary(child, "Case X", "OPV", "OPV 0", normal, currentTime.plusDays(2), currentTime.plusDays(2).plusWeeks(1));
        verify(actionService).alertForBeneficiary(child, "Case X", "BCG", "BCG", normal, currentTime.plusDays(2), currentTime.plusDays(2).plusWeeks(1));
        verify(actionService).alertForBeneficiary(child, "Case X", "Hepatitis", "HEP B0", normal, currentTime.plusDays(2), currentTime.plusDays(2).plusWeeks(1));
    }

    @Test
    public void shouldEnrollChildIntoSchedulesDuringRegistration() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        when(mothers.findByCaseId("MOTHER-CASE-1")).thenReturn(new Mother("MOTHER-CASE-1", "EC-CASE-1", "TC1"));
        ChildInformation childInformation = new ChildInformation("Case X", "MOTHER-CASE-1", "ANM X", "Child 1", "female", LocalDate.now().toString(), "", "4", "yes", EXTRA_DATA);

        service.registerChild(childInformation);

        verify(childSchedulesService).enrollChild(childInformation);
    }

    @Test
    public void shouldSaveChildIntoRepositoryDuringRegistration() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        when(mothers.findByCaseId("MOTHER-CASE-1")).thenReturn(new Mother("MOTHER-CASE-1", "EC-CASE-1", "TC 1"));

        service.registerChild(new ChildInformation("Case X", "MOTHER-CASE-1", "ANM X", "Child 1", "female", "2012-01-01", "bcg hep", "4", "yes", EXTRA_DATA));

        verify(children).register(objectWithSameFieldsAs(new Child("Case X", "EC-CASE-1", "MOTHER-CASE-1", "TC 1", "Child 1", Arrays.asList("bcg", "hep"), "female").withAnm("ANM X").withDateOfBirth("2012-01-01").withDetails(EXTRA_DATA.get("details"))));
    }

    @Test
    public void shouldNotSaveChildIntoRepositoryDuringRegistrationWhenMotherIsNotFound() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        when(mothers.findByCaseId("MOTHER-CASE-1")).thenReturn(null);

        service.registerChild(new ChildInformation("Case X", "MOTHER-CASE-1", "ANM X", "Child 1", "female", "2012-01-01", "bcg hep", "4", "yes", EXTRA_DATA));

        verifyZeroInteractions(children);
    }

    @Test
    public void shouldUpdateMotherAndChildDetailsWhenPNCVisitHappens() throws Exception {
        Map<String, String> newDetails = EXTRA_DATA.get("details");
        Map<String, String> motherUpdatedDetails = create("motherKey", "motherValue").put("someKey", "someValue").map();
        Map<String, String> childUpdatedDetails = create("childKey", "childValue").put("someKey", "someValue").map();
        String childCaseId = "Case X";
        String motherCaseId = "MOTHER-CASE-1";

        Mother mother = new Mother(motherCaseId, "EC-CASE-1", "TC 1");
        Child child = new Child(childCaseId, "EC-CASE-1", motherCaseId, "TC 1", "Child 1", Arrays.asList("bcg", "hep"), "female");

        when(mothers.exists(motherCaseId)).thenReturn(true);
        when(children.findByMotherCaseId(motherCaseId)).thenReturn(child);
        when(children.update(childCaseId, newDetails)).thenReturn(child.withDetails(childUpdatedDetails));
        when(mothers.updateDetails(motherCaseId, newDetails)).thenReturn(mother.withDetails(motherUpdatedDetails));

        service.pncVisitHappened(new PostNatalCareInformation(motherCaseId, "ANM X", "1", "50", "2012-12-12"), EXTRA_DATA);

        verify(actionService).pncVisitHappened(BeneficiaryType.mother, motherCaseId, "ANM X", LocalDate.parse("2012-12-12"), 1, "50", motherUpdatedDetails);
        verify(actionService).pncVisitHappened(BeneficiaryType.child, childCaseId, "ANM X", LocalDate.parse("2012-12-12"), 1, "50", childUpdatedDetails);
    }

    @Test
    public void shouldUpdateMotherButNotChildDetailsWhenChildIsNotFoundDuringPNCVisit() throws Exception {
        String motherCaseId = "MOTHER-CASE-1";
        when(mothers.exists(motherCaseId)).thenReturn(true);
        when(children.findByMotherCaseId(motherCaseId)).thenReturn(null);
        Map<String, String> expectedDetails = create("key", "value").put("someKey", "someValue").map();
        Map<String, String> details = mapOf("someKey", "someValue");

        when(mothers.updateDetails(motherCaseId, details)).thenReturn(new Mother(motherCaseId, "EC-CASE-1", "TC 1").withDetails(expectedDetails));

        service.pncVisitHappened(new PostNatalCareInformation(motherCaseId, "ANM X", "1", "50", "2012-12-12"), EXTRA_DATA);

        verify(actionService).pncVisitHappened(mother, motherCaseId, "ANM X", LocalDate.parse("2012-12-12"), 1, "50", expectedDetails);
    }

    @Test
    public void shouldNotDoAnythingIfMotherIsNotFoundDuringPNCVisit() throws Exception {
        String motherCaseId = "MOTHER-CASE-1";
        when(mothers.exists(motherCaseId)).thenReturn(false);

        service.pncVisitHappened(new PostNatalCareInformation(motherCaseId, "ANM X", "1", "50", "2012-12-12"), EXTRA_DATA);

        verify(mothers).exists("MOTHER-CASE-1");
        verifyNoMoreInteractions(mothers);
        verifyZeroInteractions(children);
        verifyZeroInteractions(actionService);
    }

    @Test
    public void shouldAddAlertsOnlyForMissingVaccinations() {
        assertMissingAlertsAdded("",
                asList(create("scheduleName", "BCG").put("visitCode", "BCG").map(),
                        create("scheduleName", "OPV").put("visitCode", "OPV 0").map(),
                        create("scheduleName", "Hepatitis").put("visitCode", "HEP B0").map()));
        assertMissingAlertsAdded("bcg", asList(create("scheduleName", "OPV").put("visitCode", "OPV 0").map(),
                create("scheduleName", "Hepatitis").put("visitCode", "HEP B0").map()));
        assertMissingAlertsAdded("bcg opv_0", asList(create("scheduleName", "Hepatitis").put("visitCode", "HEP B0").map()));
        assertMissingAlertsAdded("bcg opv_0 hepb_0", Collections.<Map<String, String>>emptyList());

        assertMissingAlertsAdded("opv_0 bcg hepb_0", Collections.<Map<String, String>>emptyList());
        assertMissingAlertsAdded("opv_0 bcg", asList(create("scheduleName", "Hepatitis").put("visitCode", "HEP B0").map()));
        assertMissingAlertsAdded("opv_0 bcg_1", asList(create("scheduleName", "BCG").put("visitCode", "BCG").map(),
                create("scheduleName", "Hepatitis").put("visitCode", "HEP B0").map()));
    }

    @Test
    public void shouldUpdateEnrollmentsForUpdatedImmunizations() {
        Child child = mock(Child.class);
        when(children.childExists("Case X")).thenReturn(true);
        when(children.findByCaseId("Case X")).thenReturn(child);
        when(child.immunizationsProvided()).thenReturn(asList(""));
        ChildImmunizationUpdationRequest request = new ChildImmunizationUpdationRequest("Case X", "DEMO ANM", "bcg opv0", "2012-01-01");
        when(children.update("Case X", EXTRA_DATA.get("details")))
                .thenReturn(new Child("Case X", "EC-CASE-1", "MOTHER-CASE-1", "TC 1", "Child 1", Arrays.asList("bcg", "hep"), "female")
                        .withDetails(EXTRA_DATA.get("details")));

        service.updateChildImmunization(request, EXTRA_DATA);

        verify(childSchedulesService).updateEnrollments(request);
    }

    @Test
    public void shouldUpdateChildDetailsForUpdatedImmunizations() {
        Child child = mock(Child.class);
        when(children.childExists("Case X")).thenReturn(true);
        when(children.findByCaseId("Case X")).thenReturn(child);
        when(child.immunizationsProvided()).thenReturn(asList(""));
        ChildImmunizationUpdationRequest request = new ChildImmunizationUpdationRequest("Case X", "DEMO ANM", "bcg opv0", "2012-01-01");
        when(children.update("Case X", EXTRA_DATA.get("details")))
                .thenReturn(new Child("Case X", "EC-CASE-1", "MOTHER-CASE-1", "TC 1", "Child 1", Arrays.asList("bcg", "hep"), "female")
                        .withDetails(EXTRA_DATA.get("details")));

        service.updateChildImmunization(request, EXTRA_DATA);

        verify(children).update("Case X", EXTRA_DATA.get("details"));
    }

    @Test
    public void shouldCreateActionForUpdatedImmunizations() {
        Child child = mock(Child.class);
        when(children.childExists("Case X")).thenReturn(true);
        when(children.findByCaseId("Case X")).thenReturn(child);
        when(child.immunizationsProvided()).thenReturn(asList(""));
        ChildImmunizationUpdationRequest request = new ChildImmunizationUpdationRequest("Case X", "DEMO ANM", "bcg opv0", "2012-01-01").withVitaminADose("1");
        when(children.update("Case X", EXTRA_DATA.get("details")))
                .thenReturn(new Child("Case X", "EC-CASE-1", "MOTHER-CASE-1", "TC 1", "Child 1", Arrays.asList("bcg", "hep"), "female")
                        .withDetails(EXTRA_DATA.get("details")));

        service.updateChildImmunization(request, EXTRA_DATA);

        verify(actionService).updateImmunizations("Case X", "DEMO ANM", EXTRA_DATA.get("details"), "bcg opv0", LocalDate.parse("2012-01-01"), "1");
    }

    @Test
    public void shouldNotDoAnythingIfChildIsNotFoundForUpdatedImmunizations() {
        ChildImmunizationUpdationRequest request = new ChildImmunizationUpdationRequest("Case X", "DEMO ANM", "bcg opv0", "2012-01-01").withVitaminADose("1");
        when(children.childExists("Case X")).thenReturn(false);

        service.updateChildImmunization(request, EXTRA_DATA);

        verifyZeroInteractions(actionService);
        verifyZeroInteractions(childReportingService);
        verifyZeroInteractions(childSchedulesService);
    }

    @Test
    public void shouldCallReportingServiceWithPreviousImmunizationsInsteadOfCurrentImmunizations() throws Exception {
        when(children.childExists("Case X")).thenReturn(true);
        when(children.findByCaseId("Case X")).thenReturn(new Child("Case X", "EC-CASE-1", "MOTHER-CASE-1", "TC 1", "Child 1", Arrays.asList("hep"), "female")
                .withDetails(EXTRA_DATA.get("details")));
        when(children.update("Case X", EXTRA_DATA.get("details")))
                .thenReturn(new Child("Case X", "EC-CASE-1", "MOTHER-CASE-1", "TC 1", "Child 1", Arrays.asList("hep", "bcg", "opv0"), "female")
                        .withDetails(EXTRA_DATA.get("details")));
        ChildImmunizationUpdationRequest request = new ChildImmunizationUpdationRequest("Case X", "DEMO ANM", "bcg opv0", "2012-01-01");

        service.updateChildImmunization(request, EXTRA_DATA);

        verify(childReportingService).immunizationProvided(new SafeMap(EXTRA_DATA.get("reporting")), asList("hep"));
    }

    @Test
    public void shouldCreateACloseChildActionForChildCaseClose() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        when(children.childExists("Case X")).thenReturn(true);

        service.closeChildCase(new ChildCloseRequest("Case X", "DEMO ANM"), EXTRA_DATA);

        verify(actionService).closeChild("Case X", "DEMO ANM");
    }

    @Test
    public void shouldUnenrollChildWhoseCaseHasBeenClosed() {
        when(children.childExists("Case X")).thenReturn(true);

        service.closeChildCase(new ChildCloseRequest("Case X", "ANM Y"), EXTRA_DATA);

        verify(childSchedulesService).unenrollChild("Case X");
    }

    @Test
    public void shouldReportWhenChildCaseIsClosed() {
        when(children.childExists("Case X")).thenReturn(true);

        service.closeChildCase(new ChildCloseRequest("Case X", "ANM Y"), EXTRA_DATA);

        verify(childReportingService).closeChild(new SafeMap(EXTRA_DATA.get("reporting")));
    }

    @Test
    public void shouldCloseWhenChildCaseIsClosed() {
        when(children.childExists("Case X")).thenReturn(true);

        service.closeChildCase(new ChildCloseRequest("Case X", "ANM Y"), EXTRA_DATA);

        verify(children).close("Case X");
    }

    @Test
    public void shouldNotDoAnythingIfChildDoesNotExistsDuringClose() {
        when(children.childExists("Case X")).thenReturn(false);

        service.closeChildCase(new ChildCloseRequest("Case X", "ANM Y"), EXTRA_DATA);

        verify(children).childExists("Case X");
        verifyZeroInteractions(childReportingService);
        verifyZeroInteractions(childSchedulesService);
        verifyZeroInteractions(actionService);
        verifyNoMoreInteractions(children);
    }

    @Test
    public void shouldReportWhenPNCCaseIsClosed() {
        when(mothers.exists("Case X")).thenReturn(true);

        service.closePNCCase(new PostNatalCareCloseInformation("Case X", "ANM Y", "Permanent Transfer"), EXTRA_DATA);

        verify(motherReportingService).closePNC(new SafeMap(EXTRA_DATA.get("reporting")));
    }

    @Test
    public void shouldCreateActionsWhenPNCCaseIsClosed() {
        when(mothers.exists("Case X")).thenReturn(true);

        service.closePNCCase(new PostNatalCareCloseInformation("Case X", "ANM Y", "Permanent Transfer"), EXTRA_DATA);

        verify(actionService).markAllAlertsAsInactive("Case X");
    }

    @Test
    public void shouldCloseMotherWhenPNCCaseIsClosed() {
        when(mothers.exists("Case X")).thenReturn(true);

        service.closePNCCase(new PostNatalCareCloseInformation("Case X", "ANM Y", "Permanent Transfer"), EXTRA_DATA);

        verify(mothers).close("Case X");
    }

    @Test
    public void shouldCloseECCaseAlsoWhenPNCCaseIsClosedAndReasonIsDeath() {
        when(mothers.exists("CASE-X")).thenReturn(true);

        service.closePNCCase(new PostNatalCareCloseInformation("CASE-X", "ANM X", "death_of_mother"), EXTRA_DATA);

        verify(allEligibleCouples).close("CASE-X");
    }

    @Test
    public void shouldCloseECCaseAlsoWhenPNCCaseIsClosedAndReasonIsPermanentRelocation() {
        when(mothers.exists("CASE-X")).thenReturn(true);

        service.closePNCCase(new PostNatalCareCloseInformation("CASE-X", "ANM X", "permanent_relocation"), EXTRA_DATA);

        verify(allEligibleCouples).close("CASE-X");
    }

    @Test
    public void shouldNotCloseECCaseWhenPNCCaseIsClosedAndReasonIsNeitherDeathOrPermanentRelocation() {
        when(mothers.exists("CASE-X")).thenReturn(true);

        service.closePNCCase(new PostNatalCareCloseInformation("CASE-X", "ANM X", "end_of_pp_period"), EXTRA_DATA);

        verifyZeroInteractions(ecService);
    }

    @Test
    public void shouldReportWhenPNCVisitForMotherHappens() {
        when(mothers.exists("Case X")).thenReturn(true);
        when(mothers.updateDetails("Case X", EXTRA_DATA.get("details"))).thenReturn(new Mother("Case X", "EC-CASE-1", "TC 1"));

        service.pncVisitHappened(new PostNatalCareInformation("Case X", "ANM X", "1", "50", "2012-12-12"), EXTRA_DATA);

        verify(motherReportingService).pncVisitHappened(new SafeMap(EXTRA_DATA.get("reporting")));
    }

    @Test
    public void shouldNotReportPNCVisitForMotherWhenThereIsNoMotherInDrishti() {
        when(mothers.exists("Case X")).thenReturn(false);

        service.pncVisitHappened(new PostNatalCareInformation("Case X", "ANM X", "1", "50", "2012-12-12"), EXTRA_DATA);

        verifyZeroInteractions(motherReportingService);
    }

    @Test
    public void shouldNotDoAnythingIfMotherDoesNotExistsDuringClose() {
        when(mothers.exists("Case X")).thenReturn(false);

        service.closePNCCase(new PostNatalCareCloseInformation("Case X", "ANM Y", "Permanent Transfer"), EXTRA_DATA);

        verify(mothers).exists("Case X");
        verifyZeroInteractions(actionService);
        verifyZeroInteractions(motherReportingService);
        verifyNoMoreInteractions(mothers);
    }

    @Test
    public void shouldCloseAlertsForProvidedImmunizations() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);

        Child child = mock(Child.class);
        when(children.childExists("Case X")).thenReturn(true);
        when(children.findByCaseId("Case X")).thenReturn(child);
        when(child.immunizationsProvided()).thenReturn(asList(""));
        when(children.update("Case X", EXTRA_DATA.get("details")))
                .thenReturn(new Child("Case X", "EC-CASE-1", "MOTHER-CASE-1", "TC 1", "Child 1", Arrays.asList("bcg", "hep"), "female")
                        .withDetails(EXTRA_DATA.get("details")));

        ChildImmunizationUpdationRequest updationRequest = new ChildImmunizationUpdationRequest("Case X", "DEMO ANM", "bcg dpt_1 measlesbooster", "2012-01-01").withVitaminADose("1");
        service.updateChildImmunization(updationRequest, EXTRA_DATA);

        verify(actionService).updateImmunizations("Case X", "DEMO ANM", EXTRA_DATA.get("details"), "bcg dpt_1 measlesbooster", LocalDate.parse("2012-01-01"), "1");
        for (String expectedAlert : updationRequest.immunizationsProvidedList()) {
            verify(actionService).markAlertAsClosed("Case X", "DEMO ANM", expectedAlert, "2012-01-01");
        }
        verifyNoMoreInteractions(actionService);
    }

    @Test
    public void shouldReportChildRegistration() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        SafeMap reportData = new SafeMap();
        reportData.put("caseId", "Case X");
        reportData.put("childWeight", "4");
        reportData.put("bfPostBirth", "yes");
        when(mothers.findByCaseId("MOTHER-CASE-1")).thenReturn(new Mother("MOTHER-CASE-1", "EC-CASE-1", "TC1"));
        ChildInformation childInformation = new ChildInformation("Case X", "MOTHER-CASE-1", "ANM X", "Child 1", "female", LocalDate.now().toString(), "", "4", "yes", EXTRA_DATA);

        service.registerChild(childInformation);

        verify(childReportingService).registerChild(reportData);
    }

    @Test
    public void shouldEnrollPNCIntoSchedulesDuringRegistration() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        when(mothers.exists("MOTHER-CASE-1")).thenReturn(true);

        AnteNatalCareOutcomeInformation outcomeInformation = new AnteNatalCareOutcomeInformation("MOTHER-CASE-1", "ANM X", "live_birth", "2012-01-01", "yes", "0");
        service.registerPNC(outcomeInformation);

        verify(pncSchedulesService).enrollMother(outcomeInformation);
    }

    @Test
    public void shouldNotEnrollPNCIntoSchedulesWhenMotherDoesNotExist() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        when(mothers.exists("MOTHER-CASE-1")).thenReturn(false);

        AnteNatalCareOutcomeInformation outcomeInformation = new AnteNatalCareOutcomeInformation("MOTHER-CASE-1", "ANM X", "live_birth", "2012-01-01", "yes", "0");
        service.registerPNC(outcomeInformation);

        verifyZeroInteractions(pncSchedulesService);
    }

    @Test
    public void shouldAutoClosePNCCaseWhenMotherExists() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        when(mothers.findByCaseId("MOTHER-CASE-1")).thenReturn(new Mother("MOTHER-CASE-1", "EC-CASE-1", "TC1").withAnm("ANM 1"));

        service.autoClosePNCCase("MOTHER-CASE-1");

        verify(mothers).close("MOTHER-CASE-1");
        verify(actionService).markAllAlertsAsInactive("MOTHER-CASE-1");
        verifyZeroInteractions(motherReportingService);
    }

    @Test
    public void shouldNotAutoClosePNCCaseWhenMotherDoesNotExist() {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        when(mothers.findByCaseId("MOTHER-CASE-1")).thenReturn(null);

        service.autoClosePNCCase("MOTHER-CASE-1");

        verify(mothers, times(0)).close("MOTHER-CASE-1");
        verifyZeroInteractions(actionService);
    }

    private void assertMissingAlertsAdded(String providedImmunizations, List<Map<String, String>> expectedAlertsRaised) {
        DateTime currentTime = DateUtil.now();
        mockCurrentDate(currentTime);
        ActionService actionService = mock(ActionService.class);
        PNCService pncService = new PNCService(ecService, actionService, childSchedulesService, pncSchedulesService, allEligibleCouples, mothers, children, motherReportingService, childReportingService);
        when(mothers.findByCaseId("MOTHER-CASE-1")).thenReturn(new Mother("MOTHER-CASE-1", "EC-CASE-1", "TC1"));

        pncService.registerChild(new ChildInformation("Case X", "MOTHER-CASE-1", "ANM X", "Child 1", "female", LocalDate.now().toString(), providedImmunizations, "4", "yes", EXTRA_DATA));

        for (Map<String, String> expectedAlert : expectedAlertsRaised) {
            verify(actionService).alertForBeneficiary(child, "Case X", expectedAlert.get("scheduleName"), expectedAlert.get("visitCode"), normal, currentTime.plusDays(2), currentTime.plusDays(2).plusWeeks(1));
        }
        verify(actionService, times(1)).registerChildBirth(any(String.class), any(String.class), any(String.class), any(String.class), any(LocalDate.class), any(String.class), eq(EXTRA_DATA.get("details")));
        verifyNoMoreInteractions(actionService);
    }
}
