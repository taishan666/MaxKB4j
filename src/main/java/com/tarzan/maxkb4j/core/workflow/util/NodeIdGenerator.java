package com.tarzan.maxkb4j.core.workflow.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * 节点ID生成器
 * 负责生成工作流节点的运行时唯一ID
 *
 * 使用 SHA-1 算法基于节点ID和上游节点ID列表生成哈希值
 */
@Slf4j
public class NodeIdGenerator {

    private static final String SHA_1_ALGORITHM = "SHA-1";

    private NodeIdGenerator() {
        // 工具类，不允许实例化
    }

    /**
     * 生成运行时节点ID
     *
     * 基于节点ID和上游节点ID列表生成 SHA-1 哈希值作为运行时ID
     *
     * @param nodeId         节点ID
     * @param upNodeIdList   上游节点ID列表
     * @return 运行时节点ID（SHA-1哈希值的十六进制字符串）
     * @throws RuntimeException 如果 SHA-1 算法不可用
     */
    public static String generateRuntimeNodeId(String nodeId, List<String> upNodeIdList) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_1_ALGORITHM);
            String input = Arrays.toString(upNodeIdList.toArray()) + nodeId;
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-1 算法不可用", e);
            throw new RuntimeException("SHA-1 算法不可用", e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串（小写，每个字节固定2位）
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
