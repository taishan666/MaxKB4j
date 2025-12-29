package com.tarzan.maxkb4j.core.workflow.parser;

import com.tarzan.maxkb4j.core.workflow.model.SysFile;

public interface DocumentParser {

    boolean support(SysFile sysFile);

    String handle(SysFile sysFile);
}
