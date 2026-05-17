package com.integrityfamily.member.controller;

import com.integrityfamily.member.service.MemberService;
import com.integrityfamily.security.FamilySecurityEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MemberControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @MockBean(name = "familySecurity")
    private FamilySecurityEvaluator familySecurity;

    // ---------------------------------------------------
    // getByFamily (familyId) – requires family security check
    // ---------------------------------------------------
    @Test
    @WithMockUser(roles = "USER")
    public void getByFamily_WithPermission_ShouldReturnOk() throws Exception {
        when(familySecurity.check(1L)).thenReturn(true);
        mockMvc.perform(get("/api/members/family/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getByFamily_WithoutPermission_ShouldReturnForbidden() throws Exception {
        when(familySecurity.check(1L)).thenReturn(false);
        mockMvc.perform(get("/api/members/family/1"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------
    // getById (memberId) – uses checkMember
    // ---------------------------------------------------
    @Test
    @WithMockUser(roles = "USER")
    public void getById_WithPermission_ShouldReturnOk() throws Exception {
        when(familySecurity.checkMember(5L)).thenReturn(true);
        mockMvc.perform(get("/api/members/5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getById_WithoutPermission_ShouldReturnForbidden() throws Exception {
        when(familySecurity.checkMember(5L)).thenReturn(false);
        mockMvc.perform(get("/api/members/5"))
                .andExpect(status().isForbidden());
    }
}
