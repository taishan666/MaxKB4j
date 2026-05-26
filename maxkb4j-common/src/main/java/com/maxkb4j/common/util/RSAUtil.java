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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class RSAUtil {

    private final static String PASSWORD = "mac_kb_password";
    private static final String KEY_ALGORITHM = "RSA";


    public static String byteToBase64(byte[] encoded) {
        return Base64.getEncoder().encodeToString(encoded);
    }

    // 从Base64编码的字符串恢复公钥
    public static PublicKey importPublicKey(String base64EncodedPublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedPublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    // 从Base64编码的字符串恢复私钥
    public static PrivateKey importPrivateKey(String base64EncodedPrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedPrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyGen.initialize(2048); // 指定密钥长度
        return keyGen.generateKeyPair();
    }

    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return byteToBase64(cipher.doFinal(plainText.getBytes()));
    }

    public static String encrypt(String plainText, String publicKey) throws Exception {
        return encrypt(plainText, importPublicKey(publicKey));
    }

    public static String encryptPem(String plainText, String publicKey) throws Exception {
        return encrypt(plainText, readPublicKeyPEM(publicKey));
    }

    public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
    }

    public static String decrypt(String cipherText, String privateKey) throws Exception {
        return decrypt(cipherText, importPrivateKey(privateKey));
    }


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static String readPublicKeyPEM(String pemContent) {
        // 去除PEM头尾标记和换行符
        return pemContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private static byte[] readEncryptPrivatePEM(String pemContent) {
        // 去除PEM头尾标记和换行符
        String base64Encoded = pemContent
                .replace("-----BEGIN ENCRYPTED PRIVATE KEY-----", "")
                .replace("-----END ENCRYPTED PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64Encoded);
    }


    private static String encryptPrivateKeyPem(PrivateKey privateKey, String passphrase) throws Exception {
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
        // 构建加密器
        JcePKCSPBEOutputEncryptorBuilder builder = new JcePKCSPBEOutputEncryptorBuilder(PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC);
        builder.setProvider("BC");
        OutputEncryptor encryptor = builder.build(passphrase.toCharArray());
        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new PKCS8EncryptedPrivateKeyInfoBuilder(privateKeyInfo)
                .build(encryptor);
        // 将加密后的私钥转换为PEM格式
    /*    StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            PemObject pemObject = new PemObject("ENCRYPTED PRIVATE KEY", encryptedPrivateKeyInfo.getEncoded());
            pemWriter.writeObject(pemObject);
        }*/
        return convertPEM(encryptedPrivateKeyInfo.getEncoded(), "ENCRYPTED PRIVATE KEY");
    }

    public static String convertPEM(byte[] encoded, String type) throws Exception {
        // 将加密后的私钥转换为PEM格式
        StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            PemObject pemObject = new PemObject(type, encoded);
            pemWriter.writeObject(pemObject);
        }
        return stringWriter.toString();
    }

    public static String publicKeyPem(PublicKey publicKey) throws Exception {
        return convertPEM(publicKey.getEncoded(), "PUBLIC KEY");
    }

    public static String encryptPrivateKeyPem(PrivateKey privateKey) throws Exception {
        return encryptPrivateKeyPem(privateKey, PASSWORD);
    }


    private static PrivateKey decryptPrivateKey(String encryptPrivateKey)
            throws IOException, PKCSException {
        PrivateKeyInfo pki;
        try (PEMParser pemParser = new PEMParser(new StringReader(encryptPrivateKey))) {
            Object o = pemParser.readObject();
            switch (o) {
                case PKCS8EncryptedPrivateKeyInfo epki -> {
                    //  System.out.println("key in pkcs8 encoding");
                    //  System.out.println("encryption algorithm: " + epki.getEncryptionAlgorithm().getAlgorithm());
                    JcePKCSPBEInputDecryptorProviderBuilder builder =
                            new JcePKCSPBEInputDecryptorProviderBuilder().setProvider("BC");
                    InputDecryptorProvider idp = builder.build(PASSWORD.toCharArray());
                    pki = epki.decryptPrivateKeyInfo(idp);  // encrypted private key in pkcs8-format
                }
                case PEMEncryptedKeyPair epk -> {
                    //   System.out.println("key in pkcs1 encoding");
                    PEMKeyPair pkp = epk.decryptKeyPair(new BcPEMDecryptorProvider(PASSWORD.toCharArray()));
                    pki = pkp.getPrivateKeyInfo();  // encrypted private key in pkcs1-format
                }
                case PEMKeyPair pkp ->  // unencrypted private key
                    //  System.out.println("key unencrypted");
                        pki = pkp.getPrivateKeyInfo();
                case null, default -> {
                    assert o != null;
                    throw new PKCSException("Invalid encrypted private key class: " + o.getClass().getName());
                }
            }
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            return converter.getPrivateKey(pki);
        }
    }


    public static String rsaLongDecrypt(String encryptedPem, String encryptPrivateKey) throws Exception {
        PrivateKey privateKey = decryptPrivateKey(encryptPrivateKey);
        return decrypt(encryptedPem, privateKey);
    }


    public static void main(String[] args) {
        try {
            // 生成密钥对
            KeyPair keyPair = generateRSAKeyPair();
            PublicKey publicKey1 = keyPair.getPublic();
            PrivateKey privateKey1 = keyPair.getPrivate();
            log.info("publicKey: {}", byteToBase64(publicKey1.getEncoded()));
            log.info("privateKey: {}", byteToBase64(privateKey1.getEncoded()));
            // 要加密的明文
            String originalText = "Hello, RSA Encryption!";
            PublicKey publicKey = importPublicKey(byteToBase64(publicKey1.getEncoded()));
            PrivateKey privateKey = importPrivateKey(byteToBase64(privateKey1.getEncoded()));
            // 加密
            String encryptedText = encrypt(originalText, publicKey);
            log.info("Encrypted: {}", encryptedText);

            // 解密
            String decryptedText = decrypt(encryptedText, privateKey);
            log.info("Decrypted: {}", decryptedText);

            // 验证加密和解密是否正确
            if (originalText.equals(decryptedText)) {
                log.info("Encryption and decryption were successful.");
            } else {
                log.info("Encryption and decryption failed.");
            }
        } catch (Exception e) {
            log.error("RSA test failed", e);
        }
    }
}