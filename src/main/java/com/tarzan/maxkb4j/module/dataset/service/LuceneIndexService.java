package com.tarzan.maxkb4j.module.dataset.service;

import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import lombok.AllArgsConstructor;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class LuceneIndexService {

    private final IndexWriter indexWriter;


    public void createIndex(String content,String sourceId,String paragraphId,String documentId,String datasetId,Boolean isActive) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("content", content, Field.Store.YES));
        doc.add(new StringField("sourceId", sourceId, Field.Store.NO));
        doc.add(new StringField("paragraphId", paragraphId, Field.Store.NO));
        doc.add(new StringField("paragraphId", paragraphId, Field.Store.NO));
        doc.add(new StringField("documentId", documentId, Field.Store.NO));
        doc.add(new StringField("datasetId", datasetId, Field.Store.NO));
        doc.add(new StringField("isActive", isActive.toString(), Field.Store.NO));
        indexWriter.addDocument(doc);
        indexWriter.commit(); // 提交更改
    }

    public void createIndex(List<EmbeddingEntity> entities) {
        List<Document> docs=new ArrayList<>(entities.size());
        for (EmbeddingEntity entity : entities) {
            Document doc = new Document();
            System.out.println("getContent  "+entity.getContent());
            doc.add(new TextField("content", entity.getContent(), Field.Store.YES));
            doc.add(new StoredField("sourceId", entity.getSourceId()));
            doc.add(new StoredField("paragraphId", entity.getParagraphId()));
            doc.add(new StoredField("documentId", entity.getDocumentId()));
            doc.add(new StringField("datasetId", entity.getDatasetId(), Field.Store.YES));
            doc.add(new StoredField("isActive", entity.getIsActive().toString()));
            docs.add(doc);
        }
        try {
            indexWriter.addDocuments(docs);
            indexWriter.commit(); // 提交更改
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateIndex(String sourceId, String newContent) throws IOException {
        // 首先，创建一个新的Document对象，包含更新后的内容
        Document updatedDoc = new Document();
        updatedDoc.add(new TextField("content", newContent, Field.Store.YES));
        updatedDoc.add(new StringField("sourceId", sourceId, Field.Store.NO));
        // 删除旧文档
        Term term = new Term("sourceId", sourceId);
        indexWriter.deleteDocuments(term);
        // 添加新文档到索引
        indexWriter.addDocument(updatedDoc);
        // 提交更改
        indexWriter.commit();
    }

    public void deleteIndexByParagraphId(String paragraphId) throws IOException {
        // 删除旧文档
        Term term = new Term("paragraphId", paragraphId);
        indexWriter.deleteDocuments(term);
        // 提交更改
        indexWriter.commit();
    }
    public void deleteIndex(String sourceId) throws IOException {
        // 删除旧文档
        Term term = new Term("sourceId", sourceId);
        indexWriter.deleteDocuments(term);
        // 提交更改
        indexWriter.commit();
    }

}