package com.tarzan.maxkb4j.util;

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

public class RSAUtil{

    private final static String password = "mac_kb_password";

    public static String byteToBase64(byte[] encoded) {
        return Base64.getEncoder().encodeToString(encoded);
    }

    // 从Base64编码的字符串恢复公钥
    public static PublicKey importPublicKey(String base64EncodedPublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedPublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    // 从Base64编码的字符串恢复私钥
    public static PrivateKey importPrivateKey(String base64EncodedPrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedPrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // 指定密钥长度
        return keyGen.generateKeyPair();
    }

    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return byteToBase64(cipher.doFinal(plainText.getBytes()));
    }

    public static String decrypt(String cipherText, String privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, importPrivateKey(privateKey));
        return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
    }

    public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
    }

    public static void main(String[] args) {
        try {
            // 生成密钥对
            KeyPair keyPair = generateRSAKeyPair();
            PublicKey publicKey1 = keyPair.getPublic();
            PrivateKey privateKey1 = keyPair.getPrivate();
            System.out.println("publicKey: " + byteToBase64(publicKey1.getEncoded()));
            System.out.println("privateKey: " + byteToBase64(privateKey1.getEncoded()));
            // 要加密的明文
            String originalText = "Hello, RSA Encryption!";
            PublicKey publicKey =importPublicKey(byteToBase64(publicKey1.getEncoded()));
            PrivateKey privateKey = importPrivateKey(byteToBase64(privateKey1.getEncoded()));
            // 加密
            String encryptedText = encrypt(originalText, publicKey);
            System.out.println("Encrypted: " + encryptedText);

            // 解密
            String decryptedText = decrypt(encryptedText, privateKey);
            System.out.println("Decrypted: " + decryptedText);

            // 验证加密和解密是否正确
            if (originalText.equals(decryptedText)) {
                System.out.println("Encryption and decryption were successful.");
            } else {
                System.out.println("Encryption and decryption failed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    private static byte[] readEncryptedPEM(String pemContent) throws IOException {
        // 去除PEM头尾标记和换行符
        String base64Encoded = pemContent
                .replace("-----BEGIN ENCRYPTED PRIVATE KEY-----", "")
                .replace("-----END ENCRYPTED PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64Encoded);
    }


    private static PrivateKey decryptPrivateKey1(String encodedKey, String passphrase) throws Exception {
        // 解析并解密 PKCS#8 加密私钥
        PKCS8EncryptedPrivateKeyInfo encPrivateInfo = new PKCS8EncryptedPrivateKeyInfo(readEncryptedPEM(encodedKey));
        JcePKCSPBEInputDecryptorProviderBuilder builder = new JcePKCSPBEInputDecryptorProviderBuilder().setProvider("BC");
        InputDecryptorProvider idp = builder.build(passphrase.toCharArray());
        PrivateKeyInfo privateInfo = encPrivateInfo.decryptPrivateKeyInfo(idp);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateInfo.getEncoded()));
    }

    public static String encryptPrivateKey1(PrivateKey privateKey, String passphrase) throws Exception {
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
        // 构建加密器
        JcePKCSPBEOutputEncryptorBuilder builder = new JcePKCSPBEOutputEncryptorBuilder(PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC);
        builder.setProvider("BC");
        OutputEncryptor encryptor = builder.build(passphrase.toCharArray());
        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new PKCS8EncryptedPrivateKeyInfoBuilder(privateKeyInfo)
                .build(encryptor);
        // 将加密后的私钥转换为PEM格式
        StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            PemObject pemObject = new PemObject("ENCRYPTED PRIVATE KEY", encryptedPrivateKeyInfo.getEncoded());
            pemWriter.writeObject(pemObject);
        }
        return stringWriter.toString();
    }


    private static  PrivateKey decryptPrivateKey(String encryptPrivateKey, String passphrase)
            throws IOException, PKCSException {
        PrivateKeyInfo pki;
        try (PEMParser pemParser = new PEMParser(new StringReader(encryptPrivateKey))) {
            Object o = pemParser.readObject();
            if (o instanceof PKCS8EncryptedPrivateKeyInfo) { // encrypted private key in pkcs8-format
              //  System.out.println("key in pkcs8 encoding");
                PKCS8EncryptedPrivateKeyInfo epki = (PKCS8EncryptedPrivateKeyInfo) o;
              //  System.out.println("encryption algorithm: " + epki.getEncryptionAlgorithm().getAlgorithm());
                JcePKCSPBEInputDecryptorProviderBuilder builder =
                        new JcePKCSPBEInputDecryptorProviderBuilder().setProvider("BC");
                InputDecryptorProvider idp = builder.build(passphrase.toCharArray());
                pki = epki.decryptPrivateKeyInfo(idp);
            } else if (o instanceof PEMEncryptedKeyPair) { // encrypted private key in pkcs1-format
             //   System.out.println("key in pkcs1 encoding");
                PEMEncryptedKeyPair epki = (PEMEncryptedKeyPair) o;
                PEMKeyPair pkp = epki.decryptKeyPair(new BcPEMDecryptorProvider(passphrase.toCharArray()));
                pki = pkp.getPrivateKeyInfo();
            } else if (o instanceof PEMKeyPair) { // unencrypted private key
              //  System.out.println("key unencrypted");
                PEMKeyPair pkp = (PEMKeyPair) o;
                pki = pkp.getPrivateKeyInfo();
            } else {
                throw new PKCSException("Invalid encrypted private key class: " + o.getClass().getName());
            }
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            return converter.getPrivateKey(pki);
        }
    }


    public static String rsaLongDecrypt(String encryptedPem, String encryptPrivateKey) throws Exception {
        PrivateKey privateKey = decryptPrivateKey(encryptPrivateKey, password);
/*        String sss=encryptPrivateKey1(privateKey,password);
        System.out.println("sss:\n"+sss);
        String RES=decrypt(encryptedPem,privateKey);
        System.out.println("RES:  "+RES);
        String RES1=decrypt(encryptedPem,decryptPrivateKey(sss, password));
        System.out.println("RES1:  "+RES1);*/
        return decrypt(encryptedPem,privateKey);
    }
}