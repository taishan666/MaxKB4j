package com.tarzan.maxkb4j.module.resource.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;
import com.tarzan.maxkb4j.module.resource.service.FileService;
/**
 * @author tarzan
 * @date 2025-01-21 09:34:51
 */
@RestController
@AllArgsConstructor
public class FileController{

	private	final FileService fileService;

	@GetMapping(value = "api/file/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<byte[]> editIcon(@PathVariable("id") String id){
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // 根据实际情况调整MediaType
		byte[] data =fileService.getBytes(id);
		return new ResponseEntity<>(data, headers, HttpStatus.OK);
	}
}
