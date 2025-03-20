package com.tarzan.maxkb4j.module.dataset;

import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.ArrayList;
import java.util.List;

public class MyContentRetriever implements ContentRetriever {

    private final List<ParagraphVO> paragraphList;

    public MyContentRetriever(List<ParagraphVO> paragraphList) {
        System.out.println("paragraphList size: "+paragraphList.size());
        this.paragraphList = paragraphList;
    }

    @Override
    public List<Content> retrieve(Query query) {
        List<Content> contents=new ArrayList<>();
        for (ParagraphVO ph : paragraphList) {
            contents.add(Content.from("- "+ph.getTitle()+"\n"+ph.getContent()));
        }
        return contents;
    }
}
