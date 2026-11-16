package com.integrityfamily.interop.mapper;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.interop.canonical.Household;

import java.util.List;

/** {@code Family} + sus {@code FamilyMember} → {@link Household}. */
public final class HouseholdMapper {

    private HouseholdMapper() {}

    public static Household toCanonical(Family family, List<FamilyMember> members) {
        return Household.builder()
                .canonicalId("family-" + family.getId())
                .name(family.getName())
                .members(members.stream().map(PersonMapper::toCanonical).toList())
                .municipality(family.getMunicipio())
                .departmentCode(family.getDepartmentCode())
                .countryCode(family.getCountryCode())
                .build();
    }
}
