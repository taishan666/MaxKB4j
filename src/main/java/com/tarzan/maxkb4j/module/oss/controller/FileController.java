package com.tarzan.maxkb4j.module.oss.controller;

import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author tarzan
 * @date 2025-01-21 09:34:51
 */
@RestController
@AllArgsConstructor
public class FileController{

	//private	final FileService fileService;
	private	final MongoFileService mongoFileService;

/*	@GetMapping(value = "/file/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<byte[]> editIcon(@PathVariable("id") String id){
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // 根据实际情况调整MediaType
		byte[] data =fileService.getBytes(id);
		return new ResponseEntity<>(data, headers, HttpStatus.OK);
	}*/


	@PostMapping(value = "/{prefix}/api/oss/file")
	public R<String> uploadFile(@PathVariable("prefix") String prefix, MultipartFile file) throws IOException {
		ChatFile chatFile=mongoFileService.uploadFile(file);
		return R.success(chatFile.getUrl());
	}


	@GetMapping(value = "admin/application/workspace/{appId}/oss/file/{id}")
	public void getAppFile(@PathVariable("appId") String appId, @PathVariable("id") String id, HttpServletResponse response){
		mongoFileService.getFile(id, response);
	}


	@GetMapping(value = "/{prefix}/oss/file/{id}")
	public void getFile(@PathVariable String prefix,@PathVariable("id") String id, HttpServletResponse response){
		mongoFileService.getFile(id, response);
	}


}
