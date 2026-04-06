package com.guiderag.knowledge.service;


import java.io.InputStream;

/**
 * 文档解析服务接口
 *
 * 支持的文件格式：
 * - PDF：.pdf（使用 Apache PDFBox）
 * - Word：.docx, .doc（使用 Apache POI）
 * - 纯文本：.txt, .md, .json, .xml（直接读取）
 * - 网页：.html（使用 Jsoup）
 *
 * 核心功能：
 * 1. 自动识别文件类型（基于 MIME Type 和文件扩展名）
 * 2. 提取纯文本内容（去除格式、样式、脚本）
 * 3. 保留文档结构信息（页码、段落、标题）
 * 4. 支持 OCR 识别（PDF 中的图片文字）
 */
public interface DocumentParserService {
    /**
     * 解析文档并提取文本
     *
     * 处理流程：
     * 1. 识别文件类型（通过 Apache Tika 自动检测）
     * 2. 选择对应的解析器（PDF/Word/TXT）
     * 3. 提取纯文本内容
     * 4. 清洗文本（去除控制字符、多余空白）
     * 5. 保留元数据（页码、段落分隔符）
     *
     * @param inputStream 文件输入流
     * @param fileName 文件名（用于类型判断）
     * @return 解析后的纯文本
     * @throws RuntimeException 解析失败时抛出
     */
    String parseDocument(InputStream inputStream, String fileName);
}
