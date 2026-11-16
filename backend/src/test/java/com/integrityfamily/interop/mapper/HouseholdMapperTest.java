package com.integrityfamily.interop.mapper;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.interop.canonical.Household;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HouseholdMapper")
class HouseholdMapperTest {

    @Test
    @DisplayName("mapea familia + miembros, delegando cada miembro a PersonMapper")
    void shouldMapHousehold() {
        Family family = Family.builder()
                .id(1L).name("Familia López")
                .municipio("Bogotá").departmentCode("11").countryCode("CO")
                .build();

        FamilyMember m1 = new FamilyMember();
        m1.setId(1L); m1.setFullName("Carlos"); m1.setActive(true);
        FamilyMember m2 = new FamilyMember();
        m2.setId(2L); m2.setFullName("Ana"); m2.setActive(true);

        Household household = HouseholdMapper.toCanonical(family, List.of(m1, m2));

        assertThat(household.canonicalId()).isEqualTo("family-1");
        assertThat(household.name()).isEqualTo("Familia López");
        assertThat(household.municipality()).isEqualTo("Bogotá");
        assertThat(household.departmentCode()).isEqualTo("11");
        assertThat(household.countryCode()).isEqualTo("CO");
        assertThat(household.members()).hasSize(2);
        assertThat(household.members().get(0).canonicalId()).isEqualTo("member-1");
        assertThat(household.members().get(1).canonicalId()).isEqualTo("member-2");
    }
}
