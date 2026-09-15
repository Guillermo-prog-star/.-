package com.integrityfamily.dto.home;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class FamilyHomeViewTest {

    private ObjectMapper objectMapper;
    private JsonSchema jsonSchema;

    @BeforeEach
    public void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Load the schema from classpath using JSON Schema Draft 2020-12
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        try (InputStream is = getClass().getResourceAsStream("/schemas/family-home-view.schema.json")) {
            if (is == null) {
                throw new IllegalStateException("Schema file not found in resources!");
            }
            jsonSchema = factory.getSchema(is);
        }
    }

    @Test
    public void testOnboardingViewSerializationAndSchemaValidation() throws Exception {
        // 1. Create a valid OnboardingHomeView DTO
        ViewerContext viewer = new ViewerContext(
            UUID.randomUUID(),
            ViewerRole.ADULT_MEMBER,
            List.of("VIEW_FAMILY_HOME")
        );
        MediaAsset image = new MediaAsset(
            UUID.randomUUID(),
            null, // Consent is pending, url must be null
            "Mi Familia",
            UUID.randomUUID(),
            MediaProcessingStatus.PENDING,
            ConsentStatus.PENDING,
            MediaVisibility.FAMILY_SHARED,
            null
        );
        FamilyContext family = new FamilyContext(
            UUID.randomUUID(),
            "Familia Lopez",
            image
        );
        ProgressBlock progress = new ProgressBlock(0, 4, 0);
        Command nextCommand = new Command(
            "cmd-onb",
            "Continuar registro",
            CommandType.NAVIGATE,
            "/onboarding/profile",
            true,
            false,
            null,
            null,
            null
        );
        JourneyContext journey = new JourneyContext(
            JourneyStage.NEW_FAMILY,
            progress,
            nextCommand
        );
        SafetyPresentation safety = new SafetyPresentation(
            SafetyMode.NONE,
            null,
            null,
            List.of()
        );
        ResponseMetadata meta = new ResponseMetadata(
            Instant.now(),
            Instant.now().plus(1, ChronoUnit.HOURS),
            "1.0.0",
            DataStatus.FRESH,
            UUID.randomUUID().toString()
        );
        ModuleAvailability modules = new ModuleAvailability(
            ModuleStatus.AVAILABLE,
            ModuleStatus.NOT_APPLICABLE,
            ModuleStatus.NOT_APPLICABLE
        );

        OnboardingHomeView onboardingView = new OnboardingHomeView(
            "0.9.0-Candidate",
            ViewType.ONBOARDING,
            viewer,
            family,
            journey,
            safety,
            meta,
            modules
        );

        // 2. Serialize DTO to JSON
        String json = objectMapper.writeValueAsString(onboardingView);
        assertNotNull(json);

        // 3. Validate against JSON Schema
        Set<ValidationMessage> errors = jsonSchema.validate(objectMapper.readTree(json));
        assertTrue(errors.isEmpty(), "Schema should accept a valid Onboarding DTO: " + errors);

        // 4. Test polymorphic deserialization
        FamilyHomeView deserialized = objectMapper.readValue(json, FamilyHomeView.class);
        assertEquals(ViewType.ONBOARDING, deserialized.viewType());
        assertTrue(deserialized instanceof OnboardingHomeView);
    }

    @Test
    public void testActiveHomeViewSerializationAndSchemaValidation() throws Exception {
        ViewerContext viewer = new ViewerContext(
            UUID.randomUUID(),
            ViewerRole.ADULT_MEMBER,
            List.of("VIEW_FAMILY_HOME")
        );
        MediaAsset image = new MediaAsset(
            UUID.randomUUID(),
            "https://integrityfamily.com/assets/media/image-12.jpg",
            "Nuestra Foto",
            UUID.randomUUID(),
            MediaProcessingStatus.PROCESSED,
            ConsentStatus.GRANTED,
            MediaVisibility.FAMILY_SHARED,
            Instant.now().plus(24, ChronoUnit.HOURS)
        );
        FamilyContext family = new FamilyContext(
            UUID.randomUUID(),
            "Familia Lopez",
            image
        );
        ProgressBlock journeyProgress = new ProgressBlock(3, 5, 60);
        JourneyContext journey = new JourneyContext(
            JourneyStage.ACTIVE_HOME,
            journeyProgress,
            null
        );
        SafetyPresentation safety = new SafetyPresentation(
            SafetyMode.NONE,
            null,
            null,
            List.of()
        );
        ResponseMetadata meta = new ResponseMetadata(
            Instant.now(),
            Instant.now().plus(1, ChronoUnit.HOURS),
            "1.0.0",
            DataStatus.FRESH,
            UUID.randomUUID().toString()
        );
        ModuleAvailability modules = new ModuleAvailability(
            ModuleStatus.AVAILABLE,
            ModuleStatus.AVAILABLE,
            ModuleStatus.AVAILABLE
        );

        // Active fields
        NarrativeProvenance provenance = new NarrativeProvenance(
            GeneratorType.RULE_ENGINE,
            "rule-today-home",
            null,
            List.of("sprint-4"),
            ReviewStatus.AUTO_APPROVED,
            EvidencePolicy.FAMILY_APPROVED,
            Instant.now(),
            null,
            "es",
            false
        );
        NarrativeBlock narrative = new NarrativeBlock("Esta semana están conversando más.", provenance);
        Command primaryCmd = new Command(
            "cmd-mis",
            "Hacer misión",
            CommandType.OPEN_MODAL,
            "mission-modal-4",
            true,
            false,
            null,
            null,
            null
        );
        ActiveSprintBlock activeSprint = new ActiveSprintBlock(
            SprintDisplayStatus.ACTIVE,
            UUID.randomUUID(),
            "Semana de la comunicación",
            new ProgressBlock(2, 5, 40),
            primaryCmd
        );
        DimensionBlock emotions = new DimensionBlock(DimensionStatus.STABLE, "family.dim.emotions", Instant.now(), true);
        DimensionBlock comms = new DimensionBlock(DimensionStatus.IMPROVING, "family.dim.comms", Instant.now(), true);
        DimensionBlock habits = new DimensionBlock(DimensionStatus.STABLE, "family.dim.habits", Instant.now(), false);
        DimensionBlock time = new DimensionBlock(DimensionStatus.DECLINING, "family.dim.time", Instant.now(), true);

        TodayBlock today = new TodayBlock(narrative, primaryCmd, DisplayPace.BALANCED);

        ActiveHomeView activeView = new ActiveHomeView(
            "0.9.0-Candidate",
            ViewType.ACTIVE_HOME,
            viewer,
            family,
            journey,
            safety,
            meta,
            modules,
            today,
            activeSprint,
            Map.of("emotions", emotions, "communication", comms, "habits", habits, "sharedTime", time),
            null // no resumeBlock for ACTIVE_HOME
        );

        String json = objectMapper.writeValueAsString(activeView);
        Set<ValidationMessage> errors = jsonSchema.validate(objectMapper.readTree(json));
        assertTrue(errors.isEmpty(), "Schema should accept a valid Active DTO: " + errors);

        FamilyHomeView deserialized = objectMapper.readValue(json, FamilyHomeView.class);
        assertEquals(ViewType.ACTIVE_HOME, deserialized.viewType());
        assertTrue(deserialized instanceof ActiveHomeView);
    }

    @Test
    public void testAssessmentViewSerializationAndSchemaValidation() throws Exception {
        ViewerContext viewer = new ViewerContext(UUID.randomUUID(), ViewerRole.ADULT_MEMBER, List.of("VIEW_FAMILY_HOME"));
        FamilyContext family = new FamilyContext(UUID.randomUUID(), "Familia Lopez", null);
        JourneyContext journey = new JourneyContext(
            JourneyStage.ASSESSMENT_IN_PROGRESS,
            new ProgressBlock(3, 10, 30),
            null
        );
        SafetyPresentation safety = new SafetyPresentation(SafetyMode.NONE, null, null, List.of());
        ResponseMetadata meta = new ResponseMetadata(
            Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), "1.0.0", DataStatus.FRESH, UUID.randomUUID().toString()
        );
        ModuleAvailability modules = new ModuleAvailability(
            ModuleStatus.AVAILABLE, ModuleStatus.INSUFFICIENT_DATA, ModuleStatus.NOT_APPLICABLE
        );

        AssessmentHomeView view = new AssessmentHomeView(
            "0.9.0-Candidate", ViewType.ASSESSMENT, viewer, family, journey, safety, meta, modules
        );

        String json = objectMapper.writeValueAsString(view);
        Set<ValidationMessage> errors = jsonSchema.validate(objectMapper.readTree(json));
        assertTrue(errors.isEmpty(), "Schema should accept a valid Assessment DTO: " + errors);

        FamilyHomeView deserialized = objectMapper.readValue(json, FamilyHomeView.class);
        assertEquals(ViewType.ASSESSMENT, deserialized.viewType());
        assertTrue(deserialized instanceof AssessmentHomeView);
    }

    @Test
    public void testReturnStageViewSerializationAndSchemaValidation() throws Exception {
        ViewerContext viewer = new ViewerContext(UUID.randomUUID(), ViewerRole.ADULT_MEMBER, List.of("VIEW_FAMILY_HOME"));
        FamilyContext family = new FamilyContext(UUID.randomUUID(), "Familia Lopez", null);
        Command accept = new Command(
            "cmd-ret-accept", "Aceptar Primer Sprint", CommandType.SUBMIT_ACTION,
            "accept-first-sprint", true, false, null, null, true
        );
        JourneyContext journey = new JourneyContext(
            JourneyStage.RETURN_AVAILABLE,
            new ProgressBlock(1, 1, 100),
            accept
        );
        SafetyPresentation safety = new SafetyPresentation(SafetyMode.NONE, null, null, List.of());
        ResponseMetadata meta = new ResponseMetadata(
            Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), "1.0.0", DataStatus.FRESH, UUID.randomUUID().toString()
        );
        ModuleAvailability modules = new ModuleAvailability(
            ModuleStatus.AVAILABLE, ModuleStatus.AVAILABLE, ModuleStatus.AVAILABLE
        );

        ReturnStageHomeView view = new ReturnStageHomeView(
            "0.9.0-Candidate", ViewType.RETURN_STAGE, viewer, family, journey, safety, meta, modules
        );

        String json = objectMapper.writeValueAsString(view);
        Set<ValidationMessage> errors = jsonSchema.validate(objectMapper.readTree(json));
        assertTrue(errors.isEmpty(), "Schema should accept a valid ReturnStage DTO: " + errors);

        FamilyHomeView deserialized = objectMapper.readValue(json, FamilyHomeView.class);
        assertEquals(ViewType.RETURN_STAGE, deserialized.viewType());
        assertTrue(deserialized instanceof ReturnStageHomeView);
    }

    @Test
    public void testPausedHomeViewSerializationAndSchemaValidation() throws Exception {
        // Regresión: el schema anidaba displayPace bajo "today", pero el DTO real
        // lo expone a nivel superior. Esta prueba habría fallado antes del fix.
        ViewerContext viewer = new ViewerContext(UUID.randomUUID(), ViewerRole.ADULT_MEMBER, List.of("VIEW_FAMILY_HOME"));
        FamilyContext family = new FamilyContext(UUID.randomUUID(), "Familia Lopez", null);
        JourneyContext journey = new JourneyContext(
            JourneyStage.PAUSED_HOME,
            new ProgressBlock(1, 1, 100),
            null
        );
        SafetyPresentation safety = new SafetyPresentation(SafetyMode.NONE, null, null, List.of());
        ResponseMetadata meta = new ResponseMetadata(
            Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), "1.0.0", DataStatus.FRESH, UUID.randomUUID().toString()
        );
        ModuleAvailability modules = new ModuleAvailability(
            ModuleStatus.AVAILABLE, ModuleStatus.DISABLED, ModuleStatus.DISABLED
        );

        PausedHomeView view = new PausedHomeView(
            "0.9.0-Candidate", ViewType.PAUSED_HOME, viewer, family, journey, safety, meta, modules,
            DisplayPace.PAUSED
        );

        String json = objectMapper.writeValueAsString(view);
        assertFalse(json.contains("\"today\""), "PausedHomeView no debe serializar un objeto 'today' anidado");

        Set<ValidationMessage> errors = jsonSchema.validate(objectMapper.readTree(json));
        assertTrue(errors.isEmpty(), "Schema should accept a valid PausedHome DTO: " + errors);

        FamilyHomeView deserialized = objectMapper.readValue(json, FamilyHomeView.class);
        assertEquals(ViewType.PAUSED_HOME, deserialized.viewType());
        assertTrue(deserialized instanceof PausedHomeView);
    }

    @Test
    public void testInvariantsEnforcementInDTO() {
        // Invariant: completed > total must throw Exception
        assertThrows(IllegalArgumentException.class, () -> new ProgressBlock(5, 4, 125));

        // Invariant: Consent status revoked with url must throw Exception
        assertThrows(IllegalArgumentException.class, () -> new MediaAsset(
            UUID.randomUUID(),
            "https://url.com/asset.jpg", // Url present but consent is revoked
            "Revoked Image",
            UUID.randomUUID(),
            MediaProcessingStatus.PROCESSED,
            ConsentStatus.REVOKED,
            MediaVisibility.FAMILY_SHARED,
            null
        ));

        // Invariant: Sprint Active status without todayMission must throw Exception
        assertThrows(IllegalArgumentException.class, () -> new ActiveSprintBlock(
            SprintDisplayStatus.ACTIVE,
            UUID.randomUUID(),
            "Invalid Sprint",
            new ProgressBlock(0, 1, 0),
            null // todayMission is required when active
        ));
    }
}
