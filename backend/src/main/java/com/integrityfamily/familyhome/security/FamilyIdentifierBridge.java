package com.integrityfamily.familyhome.security;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Puente entre los UUID expuestos por el contrato de Family Home y los IDs
 * Long reales del dominio (families.id, users.id). No existe columna UUID
 * persistida para Family, así que el mapeo se deriva con HMAC-SHA256 sobre
 * un secreto de configuración (nunca un salt fijo en el código fuente: un
 * salt público permitiría enumerar IDs Long offline y calcular su UUID sin
 * acceso a la base de datos). Se cachea en memoria un índice inverso
 * (UUID -> Long) reconstruible bajo demanda. Suficiente para la escala de
 * piloto actual; si el volumen de familias/usuarios crece, reemplazar por
 * una columna UUID real.
 */
@Component
public final class FamilyIdentifierBridge {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String FAMILY_NAMESPACE = "family:";
    private static final String USER_NAMESPACE = "user:";

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final SecretKeySpec key;

    private volatile Map<UUID, Long> familyIndex = Map.of();
    private volatile Map<UUID, Long> userIndex = Map.of();

    public FamilyIdentifierBridge(
            FamilyRepository familyRepository,
            UserRepository userRepository,
            @Value("${integrity.security.family-home.id-secret}") String secret) {
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public UUID toFamilyUuid(Long familyId) {
        return hmacUuid(FAMILY_NAMESPACE + familyId);
    }

    public UUID toUserUuid(Long userId) {
        return hmacUuid(USER_NAMESPACE + userId);
    }

    public Optional<Long> resolveFamilyId(UUID familyId) {
        Long id = familyIndex.get(familyId);
        if (id == null) {
            rebuildFamilyIndex();
            id = familyIndex.get(familyId);
        }
        return Optional.ofNullable(id);
    }

    public Optional<Long> resolveUserId(UUID userId) {
        Long id = userIndex.get(userId);
        if (id == null) {
            rebuildUserIndex();
            id = userIndex.get(userId);
        }
        return Optional.ofNullable(id);
    }

    private synchronized void rebuildFamilyIndex() {
        Map<UUID, Long> next = new HashMap<>();
        for (Family family : familyRepository.findAll()) {
            next.put(toFamilyUuid(family.getId()), family.getId());
        }
        familyIndex = next;
    }

    private synchronized void rebuildUserIndex() {
        Map<UUID, Long> next = new HashMap<>();
        for (User user : userRepository.findAll()) {
            next.put(toUserUuid(user.getId()), user.getId());
        }
        userIndex = next;
    }

    /**
     * Deriva un UUID determinista de {@code data} vía HMAC-SHA256, marcando los bits
     * de versión/variante como haría {@link UUID#nameUUIDFromBytes} para mantener un
     * formato reconocible (versión 5, variante RFC 4122) — sin ser invertible sin el secreto.
     */
    private UUID hmacUuid(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (digest[i] & 0xffL);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (digest[i] & 0xffL);
            }
            msb &= ~(0xfL << 12);
            msb |= 5L << 12;
            lsb &= ~(0x3L << 62);
            lsb |= 0x2L << 62;
            return new UUID(msb, lsb);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo calcular el UUID determinista (HMAC)", e);
        }
    }
}
