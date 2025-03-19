package com.tarzan.maxkb4j.module.resource.controller;

import com.tarzan.maxkb4j.module.resource.entity.ImageEntity;
import com.tarzan.maxkb4j.module.resource.service.ImageService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author tarzan
 * @date 2025-01-21 09:35:03
 */
@RestController
@AllArgsConstructor
public class ImageController{

	private	final ImageService imageService;

	@GetMapping(value = "api/image/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
	public ResponseEntity<byte[]> editIcon(@PathVariable("id") String id){
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_JPEG); // 根据实际情况调整MediaType
		ImageEntity image=imageService.getById(id);
		return new ResponseEntity<>(image.getImage(), headers, HttpStatus.OK);
	}
}
