package com.tarzan.maxkb4j.module.embedding.controller;

import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@RestController
@AllArgsConstructor
public class EmbeddingController{

	private	final EmbeddingService embeddingService;
}
