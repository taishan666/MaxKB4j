package com.tarzan.maxkb4j.module.dataset.service;

import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import lombok.AllArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class FullTextSearchService {

    private final Directory directory;

    private final Analyzer analyzer;

    public List<SearchResult> search1(List<String> datasetIds,String queryStr, int maxResults){
        try {
            DirectoryReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", analyzer); // 默认搜索content字段
            Query query = parser.parse(QueryParser.escape(queryStr)); // 转义特殊字符

            TopDocs topDocs = searcher.search(query, maxResults);
            List<SearchResult> results = new ArrayList<>();
            float maxScore = -1;
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                if (scoreDoc.score > maxScore) {
                    maxScore = scoreDoc.score;
                }
            }
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                results.add(new SearchResult(
                        doc.get("content"),
                        scoreDoc.score/maxScore
                ));
            }
            reader.close();
            return results;
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public List<HitTestVO> search(List<String> datasetIds, String queryStr, int maxResults) {
        try {
            DirectoryReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);

            // 创建一个布尔查询
            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

            // 添加文本查询条件
            QueryParser contentParser = new QueryParser("content", analyzer); // 默认搜索content字段
            Query textQuery = contentParser.parse(QueryParser.escape(queryStr)); // 转义特殊字符

            // 将文本查询添加为必须匹配的部分
            booleanQueryBuilder.add(textQuery, BooleanClause.Occur.MUST);

            // 为每个datasetId创建TermQuery，并将它们用 SHOULD 组合起来
            BooleanQuery.Builder datasetQueryBuilder = new BooleanQuery.Builder();
            for (String datasetId : datasetIds) {
                TermQuery datasetQuery = new TermQuery(new Term("datasetId", datasetId));
                datasetQueryBuilder.add(datasetQuery, BooleanClause.Occur.SHOULD);
            }

            // 将dataset查询添加为必须匹配的部分
            booleanQueryBuilder.add(datasetQueryBuilder.build(), BooleanClause.Occur.MUST);

            // 执行查询
            TopDocs topDocs = searcher.search(booleanQueryBuilder.build(), maxResults);
            List<HitTestVO> results = new ArrayList<>();
            float maxScore = 1;

            // 遍历结果，找到最高分数（如果需要的话）
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                if (scoreDoc.score > maxScore) {
                    maxScore = scoreDoc.score;
                }
            }

            // 处理和标准化得分
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                float score = scoreDoc.score / maxScore;
                results.add(new HitTestVO(
                        doc.get("paragraphId"),
                        score
                ));
            }

            reader.close();
            return results;
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public List<HitTestVO> search2(List<String> datasetIds, String queryStr, int maxResults) {
        try {
            DirectoryReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", analyzer); // 默认搜索content字段
            Query query = parser.parse(QueryParser.escape(queryStr)); // 转义特殊字符
            TopDocs topDocs = searcher.search(query, maxResults);
            List<HitTestVO> results = new ArrayList<>();
            float maxScore = 1;
            // 首先找到最高分数
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                System.out.println("score:"+scoreDoc.score);
                if (scoreDoc.score > maxScore) {
                    maxScore = scoreDoc.score;
                }
            }
            // 然后过滤和标准化得分
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                // 过滤出documentId在datasetIds中的文档
                String datasetId=doc.get("datasetId");
                if (Objects.nonNull(datasetId)&&datasetIds.contains(datasetId)) {
                    float score= scoreDoc.score/maxScore;
                    results.add(new HitTestVO(
                            doc.get("paragraphId"),
                            score
                    ));
                }
            }
            reader.close();
            return results;
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
