package com.integrityfamily.family.controller;

import com.integrityfamily.family.service.FamilyService;
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
public class FamilyControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FamilyService familyService;

    @MockBean(name = "familySecurity")
    private FamilySecurityEvaluator familySecurity;

    @Test
    @WithMockUser(roles = "USER")
    public void getAllFamilies_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/families"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getAllFamilies_AsAdmin_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/families"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getFamilyById_WithPermission_ShouldReturnOk() throws Exception {
        when(familySecurity.check(1L)).thenReturn(true);
        
        mockMvc.perform(get("/api/families/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getFamilyById_WithoutPermission_ShouldReturnForbidden() throws Exception {
        when(familySecurity.check(1L)).thenReturn(false);
        
        mockMvc.perform(get("/api/families/1"))
                .andExpect(status().isForbidden());
    }
}
