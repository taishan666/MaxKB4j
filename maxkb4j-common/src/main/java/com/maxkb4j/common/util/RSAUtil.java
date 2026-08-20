package com.maxkb4j.common.util;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.bc.BcPEMDecryptorProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfoBuilder;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEOutputEncryptorBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class RSAUtil{

    /** 密钥保护口令：优先读取系统属性/环境变量，未配置时回退默认值（兼容存量数据） */
    private final static String password = resolveKeyPassword();

    private static String resolveKeyPassword() {
        String fromProperty = System.getProperty("maxkb4j.rsa.key-password");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv("MAXKB4J_RSA_KEY_PASSWORD");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return "mac_kb_password";
    }

    /** RSA 算法名称 */
    private static final String ALGORITHM = "RSA";
    /** 新数据统一使用 OAEP 填充，避免默认 PKCS#1 v1.5 填充的预言机攻击风险 */
    private static final String TRANSFORMATION_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    /** 仅用于解密历史存量密文（JDK 默认 "RSA" 即 PKCS#1 v1.5 填充） */
    private static final String TRANSFORMATION_LEGACY = "RSA/ECB/PKCS1Padding";
    /** RSA 密钥长度 */
    private static final int KEY_SIZE = 2048;
    /** BouncyCastle 提供者名称 */
    private static final String PROVIDER = "BC";
    /** PEM 公钥头标记 */
    private static final String PEM_PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    /** PEM 公钥尾标记 */
    private static final String PEM_PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";
    /** PEM 加密私钥头标记 */
    private static final String PEM_ENCRYPTED_PRIVATE_KEY_HEADER = "-----BEGIN ENCRYPTED PRIVATE KEY-----";
    /** PEM 加密私钥尾标记 */
    private static final String PEM_ENCRYPTED_PRIVATE_KEY_FOOTER = "-----END ENCRYPTED PRIVATE KEY-----";
    /** PEM 公钥类型标识 */
    private static final String PEM_TYPE_PUBLIC_KEY = "PUBLIC KEY";
    /** PEM 加密私钥类型标识 */
    private static final String PEM_TYPE_ENCRYPTED_PRIVATE_KEY = "ENCRYPTED PRIVATE KEY";

    public static String byteToBase64(byte[] encoded) {
        return Base64.getEncoder().encodeToString(encoded);
    }

    // 从Base64编码的字符串恢复公钥
    public static PublicKey importPublicKey(String base64EncodedPublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedPublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    // 从Base64编码的字符串恢复私钥
    public static PrivateKey importPrivateKey(String base64EncodedPrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedPrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE); // 指定密钥长度
        return keyGen.generateKeyPair();
    }

    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_OAEP);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return byteToBase64(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }
    public static String encrypt(String plainText, String publicKey) throws Exception {
        return encrypt(plainText,importPublicKey(publicKey));
    }

    public static String encryptPem(String plainText, String publicKey) throws Exception {
        return encrypt(plainText,readPublicKeyPEM(publicKey));
    }

    public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
        byte[] data = Base64.getDecoder().decode(cipherText);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_OAEP);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // 回退解密历史存量数据
            Cipher legacy = Cipher.getInstance(TRANSFORMATION_LEGACY);
            legacy.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(legacy.doFinal(data), StandardCharsets.UTF_8);
        }
    }

    public static String decrypt(String cipherText, String privateKey) throws Exception {
        return decrypt(cipherText,importPrivateKey(privateKey));
    }
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static String readPublicKeyPEM(String pemContent) {
        // 去除PEM头尾标记和换行符
        return pemContent
                .replace(PEM_PUBLIC_KEY_HEADER, "")
                .replace(PEM_PUBLIC_KEY_FOOTER, "")
                .replaceAll("\\s", "");
    }

    private static byte[] readEncryptPrivatePEM(String pemContent) {
        // 去除PEM头尾标记和换行符
        String base64Encoded = pemContent
                .replace(PEM_ENCRYPTED_PRIVATE_KEY_HEADER, "")
                .replace(PEM_ENCRYPTED_PRIVATE_KEY_FOOTER, "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64Encoded);
    }




    private static PrivateKey decryptPrivateKey1(String encodedKey, String passphrase) throws Exception {
        // 解析并解密 PKCS#8 加密私钥
        PKCS8EncryptedPrivateKeyInfo encPrivateInfo = new PKCS8EncryptedPrivateKeyInfo(readEncryptPrivatePEM(encodedKey));
        JcePKCSPBEInputDecryptorProviderBuilder builder = new JcePKCSPBEInputDecryptorProviderBuilder().setProvider(PROVIDER);
        InputDecryptorProvider idp = builder.build(passphrase.toCharArray());
        PrivateKeyInfo privateInfo = encPrivateInfo.decryptPrivateKeyInfo(idp);
        return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateInfo.getEncoded()));
    }

    private static String encryptPrivateKeyPem(PrivateKey privateKey, String passphrase) throws Exception {
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
        // 构建加密器
        JcePKCSPBEOutputEncryptorBuilder builder = new JcePKCSPBEOutputEncryptorBuilder(PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC);
        builder.setProvider(PROVIDER);
        OutputEncryptor encryptor = builder.build(passphrase.toCharArray());
        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new PKCS8EncryptedPrivateKeyInfoBuilder(privateKeyInfo)
                .build(encryptor);
        // 将加密后的私钥转换为PEM格式
    /*    StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            PemObject pemObject = new PemObject("ENCRYPTED PRIVATE KEY", encryptedPrivateKeyInfo.getEncoded());
            pemWriter.writeObject(pemObject);
        }*/
        return convertPEM(encryptedPrivateKeyInfo.getEncoded(),PEM_TYPE_ENCRYPTED_PRIVATE_KEY);
    }

    public static String convertPEM(byte[] encoded,String type)  throws Exception {
        // 将加密后的私钥转换为PEM格式
        StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            PemObject pemObject = new PemObject(type, encoded);
            pemWriter.writeObject(pemObject);
        }
        return stringWriter.toString();
    }

    public static String publicKeyPem(PublicKey publicKey) throws Exception {
        return convertPEM(publicKey.getEncoded(),PEM_TYPE_PUBLIC_KEY);
    }

    public static String encryptPrivateKeyPem(PrivateKey privateKey) throws Exception {
        return encryptPrivateKeyPem(privateKey,password);
    }


    private static  PrivateKey decryptPrivateKey(String encryptPrivateKey, String passphrase)
            throws IOException, PKCSException {
        PrivateKeyInfo pki;
        try (PEMParser pemParser = new PEMParser(new StringReader(encryptPrivateKey))) {
            Object o = pemParser.readObject();
            if (o instanceof PKCS8EncryptedPrivateKeyInfo) { // encrypted private key in pkcs8-format
                PKCS8EncryptedPrivateKeyInfo epki = (PKCS8EncryptedPrivateKeyInfo) o;
                JcePKCSPBEInputDecryptorProviderBuilder builder =
                        new JcePKCSPBEInputDecryptorProviderBuilder().setProvider(PROVIDER);
                InputDecryptorProvider idp = builder.build(passphrase.toCharArray());
                pki = epki.decryptPrivateKeyInfo(idp);
            } else if (o instanceof PEMEncryptedKeyPair) { // encrypted private key in pkcs1-format
                PEMEncryptedKeyPair epki = (PEMEncryptedKeyPair) o;
                PEMKeyPair pkp = epki.decryptKeyPair(new BcPEMDecryptorProvider(passphrase.toCharArray()));
                pki = pkp.getPrivateKeyInfo();
            } else if (o instanceof PEMKeyPair) { // unencrypted private key
                PEMKeyPair pkp = (PEMKeyPair) o;
                pki = pkp.getPrivateKeyInfo();
            } else {
                throw new PKCSException("Invalid encrypted private key class: " + o.getClass().getName());
            }
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(PROVIDER);
            return converter.getPrivateKey(pki);
        }
    }


    public static String rsaLongDecrypt(String encryptedPem, String encryptPrivateKey) throws Exception {
        PrivateKey privateKey = decryptPrivateKey(encryptPrivateKey, password);
        return decrypt(encryptedPem,privateKey);
    }
}
