package com.integrityfamily.interop.fhir.mapper;

import com.integrityfamily.interop.canonical.Household;
import com.integrityfamily.interop.canonical.Person;
import org.hl7.fhir.r4.model.Group;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GroupFhirMapper")
class GroupFhirMapperTest {

    @Test
    @DisplayName("mapea tipo PERSON, miembros como referencias Patient/ y ubicación como extensions")
    void shouldMapHousehold() {
        Person p1 = Person.builder().canonicalId("member-1").identifiers(List.of()).fullName("Carlos").active(true).build();
        Person p2 = Person.builder().canonicalId("member-2").identifiers(List.of()).fullName("Ana").active(true).build();

        Household household = Household.builder()
                .canonicalId("family-1")
                .name("Familia López")
                .members(List.of(p1, p2))
                .municipality("Bogotá")
                .departmentCode("11")
                .countryCode("CO")
                .build();

        Group group = GroupFhirMapper.toFhir(household);

        assertThat(group.getId()).isEqualTo("family-1");
        assertThat(group.getType()).isEqualTo(Group.GroupType.PERSON);
        assertThat(group.getActual()).isTrue();
        assertThat(group.getName()).isEqualTo("Familia López");
        assertThat(group.getMember()).hasSize(2);
        assertThat(group.getMember().get(0).getEntity().getReference()).isEqualTo("Patient/member-1");
        assertThat(group.getExtension()).hasSize(3);
    }

    @Test
    @DisplayName("sin ubicación → sin extensions")
    void shouldSkipExtensions_whenNoLocation() {
        Household household = Household.builder()
                .canonicalId("family-2").name("Familia X").members(List.of())
                .build();

        Group group = GroupFhirMapper.toFhir(household);

        assertThat(group.getExtension()).isEmpty();
        assertThat(group.getMember()).isEmpty();
    }
}
