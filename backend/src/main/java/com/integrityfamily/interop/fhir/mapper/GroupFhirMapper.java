package com.integrityfamily.interop.fhir.mapper;

import com.integrityfamily.interop.canonical.Household;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

/**
 * {@link Household} → FHIR {@code Group} (recurso piloto de la Fase 4). FHIR
 * no tiene un recurso "Family" — Group con {@code type=PERSON} es la
 * representación estándar (ver punto 7 del diseño original de este programa).
 *
 * {@code Group} no tiene un elemento {@code address} en R4 (a diferencia de
 * Patient/Location/Organization), así que municipio/departamento/país se
 * publican como extensiones propias, no como un campo nativo forzado.
 */
public final class GroupFhirMapper {

    private static final String MUNICIPALITY_EXT = "https://integrityfamily.com/fhir/StructureDefinition/household-municipality";
    private static final String DEPARTMENT_EXT = "https://integrityfamily.com/fhir/StructureDefinition/household-department-code";
    private static final String COUNTRY_EXT = "https://integrityfamily.com/fhir/StructureDefinition/household-country-code";

    private GroupFhirMapper() {}

    public static Group toFhir(Household household) {
        Group group = new Group();
        group.setId(household.canonicalId());
        group.setType(Group.GroupType.PERSON);
        group.setActual(true);
        group.setName(household.name());

        for (var member : household.members()) {
            group.addMember(new Group.GroupMemberComponent()
                    .setEntity(new Reference("Patient/" + member.canonicalId())));
        }

        if (household.municipality() != null) {
            group.addExtension(MUNICIPALITY_EXT, new StringType(household.municipality()));
        }
        if (household.departmentCode() != null) {
            group.addExtension(DEPARTMENT_EXT, new StringType(household.departmentCode()));
        }
        if (household.countryCode() != null) {
            group.addExtension(COUNTRY_EXT, new StringType(household.countryCode()));
        }

        return group;
    }
}
