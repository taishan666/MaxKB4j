package com.tarzan.maxkb4j;

import com.tarzan.maxkb4j.module.dataset.service.LuceneIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
public class MaxKb4jApplication  {

    @Autowired
    private LuceneIndexService luceneIndexService;

    public static void main(String[] args) {
        SpringApplication.run(MaxKb4jApplication.class, args);
    }

  /*  @Override
    public void run(String... args) throws Exception {
        indexService.indexDocument("Spring Boot使Java应用开发变得简单。");
        indexService.indexDocument("Lucene是一个强大的全文检索。");
        indexService.indexDocument("Lucene是一个强大的全文检索引擎。");
        indexService.indexDocument("Lucene是一个强大的全文检索引擎库。");
        indexService.indexDocument( "测试一下精度");
        indexService.indexDocument("测试一下精度1");
        indexService.indexDocument("测试一下精度2");
    }*/

}
