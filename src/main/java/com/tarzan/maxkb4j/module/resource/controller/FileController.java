package com.tarzan.maxkb4j.module.resource.controller;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.core.workflow.domain.ChatFile;
import com.tarzan.maxkb4j.module.resource.service.MongoFileService;
import com.tarzan.maxkb4j.util.IoUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author tarzan
 * @date 2025-01-21 09:34:51
 */
@Slf4j
@RestController
@RequestMapping(AppConst.ADMIN_PATH)
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


	@PostMapping(value = "oss/file")
	public R<String> uploadFile(MultipartFile file) throws IOException {
		ChatFile chatFile=mongoFileService.uploadFile(file);
		return R.success(chatFile.getUrl());
	}

	@GetMapping(value = "/file/{id}")
	public void getFile(@PathVariable("id") String id, HttpServletResponse response){
		try {
			GridFSFile file = mongoFileService.getById(id);
			Document doc=file.getMetadata();
			// 获取文件的MIME类型
			assert doc != null;
			String mimeType = doc.getString("_contentType");
			response.setContentType(mimeType);
			String encodedFileName = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8).replace("+", "%20");
			response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
					"inline; filename*=UTF-8''" + encodedFileName);
			IoUtil.copy(mongoFileService.getStream(file), response.getOutputStream());
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
}
