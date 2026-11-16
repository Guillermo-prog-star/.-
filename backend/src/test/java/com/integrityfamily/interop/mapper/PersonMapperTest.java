package com.integrityfamily.interop.mapper;

import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.interop.canonical.Person;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PersonMapper")
class PersonMapperTest {

    @Test
    @DisplayName("con documento → agrega CanonicalIdentifier con system=documentType")
    void shouldMapWithIdentifier() {
        FamilyMember member = new FamilyMember();
        member.setId(10L);
        member.setFullName("Ana Martínez");
        member.setFirstName("Ana");
        member.setEmail("ana@test.com");
        member.setPhone("3001234567");
        member.setRole("MADRE");
        member.setBirthDate(LocalDate.of(1985, 5, 20));
        member.setActive(true);
        member.setDocumentType("CC");
        member.setDocumentNumber("1020304050");

        Person person = PersonMapper.toCanonical(member);

        assertThat(person.canonicalId()).isEqualTo("member-10");
        assertThat(person.fullName()).isEqualTo("Ana Martínez");
        assertThat(person.familyRole()).isEqualTo("MADRE");
        assertThat(person.birthDate()).isEqualTo(LocalDate.of(1985, 5, 20));
        assertThat(person.active()).isTrue();
        assertThat(person.identifiers()).hasSize(1);
        assertThat(person.identifiers().get(0).system()).isEqualTo("CC");
        assertThat(person.identifiers().get(0).value()).isEqualTo("1020304050");
    }

    @Test
    @DisplayName("sin documento → lista de identificadores vacía, no null")
    void shouldMapWithoutIdentifier() {
        FamilyMember member = new FamilyMember();
        member.setId(11L);
        member.setFullName("Luisa");
        member.setActive(true);

        Person person = PersonMapper.toCanonical(member);

        assertThat(person.identifiers()).isEmpty();
    }
}
