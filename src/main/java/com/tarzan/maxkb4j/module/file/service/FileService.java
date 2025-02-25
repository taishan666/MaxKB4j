package com.tarzan.maxkb4j.module.file.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.file.entity.FileEntity;
import com.tarzan.maxkb4j.module.file.mapper.FileMapper;
import com.tarzan.maxkb4j.module.file.vo.FileVO;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author tarzan
 * @date 2025-01-21 09:34:51
 */
@Service
@AllArgsConstructor
public class FileService extends ServiceImpl<FileMapper, FileEntity>{

    private final JdbcTemplate jdbcTemplate;

    public JSONObject uploadFile(String fileName, byte[] fileBytes) {
        JSONObject vo=new JSONObject();
        String sql = "SELECT lo_from_bytea(?, ?::bytea) AS loid";
        int loid;
        loid = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getInt("loid"), 0, fileBytes);
        FileEntity fileEntity=new FileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setLoid(loid);
        fileEntity.setMeta(new JSONObject());
        save(fileEntity);
        vo.put("file_id",fileEntity.getId());
        vo.put("name",fileName);
        vo.put("url","/api/file/"+fileEntity.getId());
        return vo;
    }
    public FileVO uploadFile1(String fileName, byte[] fileBytes) {
        FileVO vo=new FileVO();
        String sql = "SELECT lo_from_bytea(?, ?::bytea) AS loid";
        int loid;
        loid = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getInt("loid"), 0, fileBytes);
        FileEntity fileEntity=new FileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setLoid(loid);
        fileEntity.setMeta(new JSONObject());
        save(fileEntity);
        vo.setFileId(fileEntity.getId());
        vo.setName(fileName);
        vo.setUrl("/api/file/"+fileEntity.getId());
        return vo;
    }

    public void updateFile(String fileId, byte[] fileBytes) {
        String sql = "SELECT lo_from_bytea(?, ?::bytea) AS loid";
        int loid;
        loid = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getInt("loid"), 0, fileBytes);
        FileEntity fileEntity=new FileEntity();
        fileEntity.setId(fileId);
        fileEntity.setLoid(loid);
        updateById(fileEntity);
    }

    public byte[] getBytes(String id) {
        FileEntity file=getById(id);
        String sql = "SELECT lo_get(?) AS data";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getBytes("data"), file.getLoid());
    }

}
