package com.tarzan.maxkb4j.module.file.controller;

import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;
import com.tarzan.maxkb4j.module.file.service.FileService;
/**
 * @author tarzan
 * @date 2025-01-21 09:34:51
 */
@RestController
@AllArgsConstructor
public class FileController{

	private	final FileService fileService;
}
