package com.integrityfamily.member.service;

import com.integrityfamily.family.domain.Member;
import com.integrityfamily.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public List<Member> findByFamily(Long familyId) {
        return memberRepository.findByFamilyId(familyId);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Miembro de la familia no encontrado"));
    }

    @Transactional
    public Member create(Member member) {
        return memberRepository.save(member);
    }

    /**
     * createInFamily: Método solicitado por MemberController para vincular
     * integrantes a un nodo familiar específico.
     */
    @Transactional
    public Member createInFamily(Long familyId, Member member) {
        // En una implementación avanzada, aquí buscarías la entidad Family
        // y harías member.setFamily(family). Por ahora, guardamos la entidad.
        return memberRepository.save(member);
    }

    @Transactional
    public Member update(Long id, Member request) {
        Member existing = findById(id);
        
        // SINCRONIZACIÓN FINAL: Usamos los nombres exactos de Member.java
        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setRelationship(request.getRelationship());
        existing.setAge(request.getAge());
        existing.setGender(request.getGender());
        existing.setOccupation(request.getOccupation());
        existing.setRole(request.getRole());
        existing.setActive(request.isActive());
        
        return memberRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}