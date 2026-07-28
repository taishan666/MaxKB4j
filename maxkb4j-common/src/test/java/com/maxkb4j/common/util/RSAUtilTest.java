package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归测试：RSA 密钥生成 / Base64 导入导出 / 加解密往返。
 */
class RSAUtilTest {

    @Test
    void byteToBase64_encodesBytes() {
        assertThat(RSAUtil.byteToBase64(new byte[]{1, 2, 3}))
                .isEqualTo(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}));
    }

    @Test
    void generateRSAKeyPair_returnsValidPair() throws Exception {
        KeyPair keyPair = RSAUtil.generateRSAKeyPair();
        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic().getEncoded()).isNotEmpty();
        assertThat(keyPair.getPrivate().getEncoded()).isNotEmpty();
    }

    @Test
    void importKeys_recoversFromBase64() throws Exception {
        KeyPair keyPair = RSAUtil.generateRSAKeyPair();
        String pubB64 = RSAUtil.byteToBase64(keyPair.getPublic().getEncoded());
        String priB64 = RSAUtil.byteToBase64(keyPair.getPrivate().getEncoded());

        PublicKey importedPub = RSAUtil.importPublicKey(pubB64);
        PrivateKey importedPri = RSAUtil.importPrivateKey(priB64);

        assertThat(importedPub).isEqualTo(keyPair.getPublic());
        assertThat(importedPri).isEqualTo(keyPair.getPrivate());
    }

    @Test
    void encryptThenDecrypt_roundTripsWithKeyObjects() throws Exception {
        KeyPair keyPair = RSAUtil.generateRSAKeyPair();
        String cipher = RSAUtil.encrypt("secret message", keyPair.getPublic());
        assertThat(cipher).isNotEqualTo("secret message");
        assertThat(RSAUtil.decrypt(cipher, keyPair.getPrivate())).isEqualTo("secret message");
    }

    @Test
    void encryptThenDecrypt_roundTripsWithBase64Keys() throws Exception {
        KeyPair keyPair = RSAUtil.generateRSAKeyPair();
        String pubB64 = RSAUtil.byteToBase64(keyPair.getPublic().getEncoded());
        String priB64 = RSAUtil.byteToBase64(keyPair.getPrivate().getEncoded());

        String cipher = RSAUtil.encrypt("hello rsa", pubB64);
        assertThat(RSAUtil.decrypt(cipher, priB64)).isEqualTo("hello rsa");
    }

    @Test
    void importPublicKey_invalidBase64Throws() {
        assertThatThrownBy(() -> RSAUtil.importPublicKey("!!!不是合法Base64!!!"))
                .isInstanceOf(Exception.class);
    }
}