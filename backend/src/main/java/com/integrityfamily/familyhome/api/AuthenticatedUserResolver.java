package com.integrityfamily.familyhome.api;

import java.util.UUID;

public interface AuthenticatedUserResolver {
    UUID requireAuthenticatedUserId();
}
