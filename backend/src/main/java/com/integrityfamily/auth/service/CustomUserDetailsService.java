package com.integrityfamily.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;

/**
 * @deprecated MODULAR DUPLICATE. Use com.integrityfamily.security.CustomUserDetailsService instead.
 * Neutralized to avoid BeanDefinitionStoreException.
 */
//@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Deprecated
public class CustomUserDetailsService {
    // This class is no longer a Spring Bean. 
    // It exists only to avoid breaking legacy references during the final cleanup.
}
